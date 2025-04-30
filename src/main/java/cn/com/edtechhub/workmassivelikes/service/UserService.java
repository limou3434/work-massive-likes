package cn.com.edtechhub.workmassivelikes.service;

import cn.com.edtechhub.workmassivelikes.model.dto.UserDto;
import cn.com.edtechhub.workmassivelikes.model.entity.User;
import cn.com.edtechhub.workmassivelikes.request.UserAddRequest;
import cn.com.edtechhub.workmassivelikes.request.UserDeleteRequest;
import cn.com.edtechhub.workmassivelikes.request.UserSearchRequest;
import cn.com.edtechhub.workmassivelikes.request.UserUpdateRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 用户服务声明
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户添加服务
     */
    User userAdd(UserAddRequest userAddRequest);

    /**
     * 用户删除服务
     */
    Boolean userDelete(UserDeleteRequest userDeleteRequest);

    /**
     * 用户更新服务
     */
    User userUpdate(UserUpdateRequest userUpdateRequest);

    /**
     * 用户查询服务
     */
    List<User> userSearch(UserSearchRequest userSearchRequest);

    /**
     * 用户封禁服务
     */
    Boolean userDisable(Long userId, Long disableTime);

    /**
     * 用户注册服务
     */
    Boolean userRegister(String account, String passwd, String checkPasswd);

    /**
     * 用户登入服务
     */
    User userLogin(String account, String passwd, String device);

    /**
     * 用户登出服务
     */
    Boolean userLogout(String device);

    /**
     * 用户状态服务
     */
    UserDto userStatus();

}
