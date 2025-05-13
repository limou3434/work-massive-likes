package cn.com.edtechhub.workmassivelikes.service.impl;

import cn.com.edtechhub.workmassivelikes.enums.UserRoleEnum;
import cn.com.edtechhub.workmassivelikes.model.entity.User;
import cn.com.edtechhub.workmassivelikes.request.UserSearchRequest;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import cn.dev33.satoken.stp.StpInterface;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限认证服务层实现
 * 保证此类被 SpringBoot 扫描, 完成 Sa-Token 的自定义权限验证扩展
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@Component // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
@Slf4j
public class SaTokenStpInterfaceImpl implements StpInterface {

    /**
     * 注入用户服务实例
     */
    @Resource
    private UserService userService;

    /**
     * 返回一个账号所拥有的权限码值集合(暂时没有用到)
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) { // loginType 可以用来区分不同客户端
        Long userId = Long.valueOf(loginId.toString());
        List<String> list = new ArrayList<>();
        list.add("*"); // 有需要再实现
        return list;
    }

    /**
     * 返回一个账号所拥有的角色标识集合(权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) { // loginType 可以用来区分不同客户端
        // 获取登录用户的 id
        Long userId = Long.valueOf(loginId.toString()); // loginId 来源自使用 sa-token 登录接口时使用的 loginId
        // 根据 id 值查询用户的信息, 最主要是为了获取身份字段
        UserSearchRequest userSearchRequest = new UserSearchRequest();
        userSearchRequest.setId(userId);
        List<User> users = userService.userSearch(userSearchRequest);

        // 由于在本数据库中为了拓展性使用数字来标识身份, 因此需要做一层转化
        String userRole = UserRoleEnum.getUserDescription(users.get(0).getRole());
        log.debug("检测一次当前用户的身份名称: {}", userRole);

        List<String> list = new ArrayList<>();
        list.add(userRole);
        return list;
    }

}
