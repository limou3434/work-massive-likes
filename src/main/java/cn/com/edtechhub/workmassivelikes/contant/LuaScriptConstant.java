package cn.com.edtechhub.workmassivelikes.contant;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

public class LuaScriptConstant {

    /**
     * 确认点赞 Lua 脚本
     */
    public static final RedisScript<Long> THUMB_SCRIPT = new DefaultRedisScript<>("""  
            local tempThumbKey = KEYS[1] -- 临时点赞记录键名(如 thumb:temp:{time_slice}) => thumb:temp:11:20:00 -> "01:001=-1", "01:002=1"
            local userThumbKey = KEYS[2] -- 用户点赞记录键名(如 thumb:{user_id})         =>            thumb:01 -> "001=0001", "002=0002"
            local userId = ARGV[1]       -- 用户 ID
            local blogId = ARGV[2]       -- 博客 ID
            
            -- 1. 检查是否已经确认点赞(避免重复操作)
            if redis.call('HEXISTS', userThumbKey, blogId) == 1 then
                return -1  -- 已点赞, 返回 -1 表示"已经确认点赞"
            end
            
            -- 2. 获取临时点赞记录旧值(不存在则默认为 0)
            local hashKey = userId .. ':' .. blogId
            local oldNumber = tonumber(redis.call('HGET', tempThumbKey, hashKey) or 0)
            
            -- 3. 计算新值
            local newNumber = oldNumber + 1
            
            -- 4. 更新
            redis.call('HSET', tempThumbKey, hashKey, newNumber) -- 更新临时点赞记录键值对
            redis.call('HSET', userThumbKey, blogId, 1)          -- 写入用户点赞记录键值对
            
            return 1  -- 返回 1 表示"确认点赞"
            """, Long.class);

    /**
     * 取消点赞 Lua 脚本
     */
    public static final RedisScript<Long> UNTHUMB_SCRIPT = new DefaultRedisScript<>("""  
            local tempThumbKey = KEYS[1] -- 临时点赞记录键名(如 thumb:temp:{time_slice}) => thumb:temp:11:20:00 -> "01:001=-1", "01:002=1"
            local userThumbKey = KEYS[2] -- 用户点赞记录键名(如 thumb:{user_id})         =>            thumb:01 -> "001=1", "002=1"
            local userId = ARGV[1]       -- 用户 ID
            local blogId = ARGV[2]       -- 博客 ID
            
            -- 1. 检查是否已经取消点赞(避免重复操作)
            if redis.call('HEXISTS', userThumbKey, blogId) ~= 1 then
                return -1  -- 未点赞，返回 -1 表示失败
            end
            
            -- 2. 获取临时点赞记录旧值(不存在则默认为 0)
            local hashKey = userId .. ':' .. blogId
            local oldNumber = tonumber(redis.call('HGET', tempThumbKey, hashKey) or 0)
            
            -- 3. 计算新值
            local newNumber = oldNumber - 1
            
            -- 4. 更新
            redis.call('HSET', tempThumbKey, hashKey, newNumber) -- 更新临时点赞记录键值对
            redis.call('HDEL', userThumbKey, blogId)             -- 删除用户点赞记录键值对
            
            return 1  -- 返回 1 表示成功
            """, Long.class);
}
