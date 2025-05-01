package cn.com.edtechhub.workmassivelikes.service.impl;

import cn.com.edtechhub.workmassivelikes.config.MyBatisPlusConfig;
import cn.com.edtechhub.workmassivelikes.contant.UserConstant;
import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessageEnum;
import cn.com.edtechhub.workmassivelikes.exception.BusinessException;
import cn.com.edtechhub.workmassivelikes.mapper.UserMapper;
import cn.com.edtechhub.workmassivelikes.model.dto.UserStatusDto;
import cn.com.edtechhub.workmassivelikes.model.entity.User;
import cn.com.edtechhub.workmassivelikes.request.UserAddRequest;
import cn.com.edtechhub.workmassivelikes.request.UserDeleteRequest;
import cn.com.edtechhub.workmassivelikes.request.UserSearchRequest;
import cn.com.edtechhub.workmassivelikes.request.UserUpdateRequest;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private MyBatisPlusConfig mybatisPlusConfig;

    @Override
    public User userAdd(UserAddRequest userAddRequest) {
        checkAccountAndPasswd(userAddRequest.getAccount(), userAddRequest.getPasswd());
        var user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        String passwd = user.getPasswd().isEmpty() ? UserConstant.DEFAULT_PASSWD : user.getPasswd(); // 如果密码为空则需要设置默认密码
        user.setPasswd(DigestUtils.md5DigestAsHex((mybatisPlusConfig.getSalt() + passwd).getBytes())); // 需要加密密码
        try {
            this.save(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(CodeBindMessageEnum.OPERATION_ERROR, "已经存在该用户, 或者曾经被删除");
        }
        return user;
    }

    @Override
    public Boolean userDelete(UserDeleteRequest userDeleteRequest) {
        if (userDeleteRequest.getId() == null || userDeleteRequest.getId() <= 0) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "参数用户 id 不合法");
        }
        return this.removeById(userDeleteRequest.getId()); // 这里 MyBatisPlus 会自动转化为逻辑删除
    }

    @Override
    public User userUpdate(UserUpdateRequest userUpdateRequest) { // TODO: 全量更新还是有点底效率
        if (userUpdateRequest.getId() == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "参数用户 id 不合法");
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);

        if (StringUtils.isNotBlank(user.getPasswd())) {
            user.setPasswd(DigestUtils.md5DigestAsHex((mybatisPlusConfig.getSalt() + user.getPasswd()).getBytes())); // 需要加密密码 TODO: 这里有个雷, 如果用户的密码被查询出来, 就会导致再次加密, 暂时使用 if 解决
        }

        this.updateById(user);
        return user;
    }

    @Override
    public List<User> userSearch(UserSearchRequest userSearchRequest) {
        Page<User> page = new Page<>(userSearchRequest.getPageCurrent(), userSearchRequest.getPageSize()); // 创建分页对象, 指定页码和每页条数
        LambdaQueryWrapper<User> queryWrapper = this.getQueryWrapper(userSearchRequest); // 构造查询条件
        Page<User> userPage = this.page(page, queryWrapper); // 调用 MyBatis-Plus 的分页查询方法
        return userPage.getRecords(); // 返回分页结果
    }

    @Override
    public Boolean userDisable(Long userId, Long disableTime) {
        // 查询数据库
        UserSearchRequest userSearchRequest = new UserSearchRequest();
        userSearchRequest.setId(userId);
        userSearchRequest.setPageCurrent(1);
        userSearchRequest.setPageSize(1);
        User user = this.userSearch(userSearchRequest).get(0);
        user.setPasswd(null); // 暂时这么做以避免密码被二次加密

        // 复制用户原本的信息到更新请求实例中
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
        BeanUtils.copyProperties(user, userUpdateRequest);

        // 如果封禁时间为 0 则表示取消封禁, 默认解封后设置为普通用户权限, 否则封禁用户
        if (disableTime == 0) {
            StpUtil.untieDisable(userId);
            userUpdateRequest.setRole(0);
        } else {
            StpUtil.kickout(userId); // 先踢下线
            StpUtil.disable(userId, disableTime); // 然后再进行封禁 TODO: 可以做一些动态封禁, 比如先封禁 1 天、3 天、5 天、...
            userUpdateRequest.setRole(-1);
        }

        // 把封禁体现到数据库中方便维护
        this.userUpdate(userUpdateRequest);
        return true;
    }

    @Override
    public Boolean userRegister(String account, String passwd, String checkPasswd) {
        checkAccountAndPasswd(account, passwd);
        if (!passwd.equals(checkPasswd)) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "两次输入的密码不一致");
        }
        UserAddRequest userAddRequest = new UserAddRequest();
        userAddRequest.setAccount(account);
        userAddRequest.setPasswd(passwd);
        this.userAdd(userAddRequest);

        return true;
    }

    @Override
    public User userLogin(String account, String passwd, String device) {
        checkAccountAndPasswd(account, passwd);

        // 查询对于用户
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getAccount, account).eq(User::getPasswd, DigestUtils.md5DigestAsHex((mybatisPlusConfig.getSalt() + passwd).getBytes()));
        User user = this.getOne(lambdaQueryWrapper);
        if (user == null) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "该用户可能不存在, 也可能是密码错误");
        }

        // 先检查是否被封号再来登录
        StpUtil.checkDisable(user.getId()); // 这个方法检测封号就会抛出异常
        StpUtil.login(user.getId(), device);
        log.debug("检测一次设备类型是否真的被设置: {}", StpUtil.getLoginDevice());

        return user;
    }

    @Override
    public Boolean userLogout(String device) {
        StpUtil.logout();
        log.debug("检测一次设备类型是否真的被取消: {}", StpUtil.getLoginDevice());
        return true;
    }

    @Override
    public UserStatusDto userStatus() {
        UserStatusDto userStatusDto = new UserStatusDto();
        userStatusDto.setIsLogin(StpUtil.isLogin());
        if (!userStatusDto.getIsLogin()) {
            return userStatusDto;
        }
        userStatusDto.setTokenName(StpUtil.getTokenName());
        userStatusDto.setTokenTimeout(String.valueOf(StpUtil.getTokenTimeout()));
        userStatusDto.setUserId(String.valueOf(StpUtil.getLoginId()));
        return userStatusDto;
    }

    /**
     * 检查账户和密码是否合规的方法
     */
    private void checkAccountAndPasswd(String checkAccount, String checkPasswd) {
        // 账户和密码都不能为空
        if (StringUtils.isAllBlank(checkAccount)) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "账户为空");
        }
        if (StringUtils.isAllBlank(checkPasswd)) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "密码为空");
        }

        // 判断账户和密码的长度是否符合要求
        if (checkAccount.length() < UserConstant.ACCOUNT_LENGTH) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "账户不得小于" + UserConstant.ACCOUNT_LENGTH + "位");
        }
        if (checkPasswd.length() < UserConstant.PASSWD_LENGTH) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "密码不得小于" + UserConstant.PASSWD_LENGTH + "位");
        }

        // 避免账户和密码中的非法字符
        String validPattern = "^[$_-]+$";
        if (checkAccount.matches(validPattern)) {
            throw new BusinessException(CodeBindMessageEnum.PARAMS_ERROR, "账号不能包含特殊字符");
        }
    }

    /**
     * 获取查询封装器的方法
     */
    private LambdaQueryWrapper<User> getQueryWrapper(UserSearchRequest userSearchRequest) {
        // 取得需要查询的参数
        Long id = userSearchRequest.getId();
        String account = userSearchRequest.getAccount();
        Integer role = userSearchRequest.getRole();
        Integer level = userSearchRequest.getLevel();
        String sortOrder = userSearchRequest.getSortOrder();
        String sortField = userSearchRequest.getSortField();

        // 获取包装器进行返回
        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(id != null, User::getId, id);
        lambdaQueryWrapper.eq(StringUtils.isNotBlank(account), User::getAccount, account);
        lambdaQueryWrapper.eq(role != null, User::getRole, role);
        lambdaQueryWrapper.eq(level != null, User::getLevel, level);
        lambdaQueryWrapper.orderBy(
                StringUtils.isNotBlank(sortField) && !StringUtils.containsAny(sortField, "=", "(", ")", " "),
                sortOrder.equals("ascend"), // 这里结果为 true 代表 ASC 升序, false 代表 DESC 降序
                User::getAccount // 默认按照账户排序
        );
        return lambdaQueryWrapper;
    }

}




