package cn.com.edtechhub.workmassivelikes.controller;

import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;
import cn.com.edtechhub.workmassivelikes.model.dto.UserDto;
import cn.com.edtechhub.workmassivelikes.model.entity.User;
import cn.com.edtechhub.workmassivelikes.model.vo.UserVO;
import cn.com.edtechhub.workmassivelikes.request.*;
import cn.com.edtechhub.workmassivelikes.response.BaseResponse;
import cn.com.edtechhub.workmassivelikes.response.TheResult;
import cn.com.edtechhub.workmassivelikes.service.UserService;
import cn.com.edtechhub.workmassivelikes.utils.DeviceUtil;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制层
 *
 * @author <a href="https://github.com/limou3434">limou3434</a>
 */
@RestController // 返回值默认为 json 类型
@RequestMapping("/user")
@Slf4j
public class UserController { // 通常控制层有服务层中的所有方法, 并且还有组合而成的方法, 如果组合的方法开始变得复杂就会封装到服务层内部

    /**
     * 注入用户服务实例
     */
    @Resource
    private UserService userService;

    /**
     * 添加用户网络接口
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @PostMapping("/add")
    public BaseResponse<User> userAdd(@RequestBody UserAddRequest userAddRequest) {
        User user = userService.userAdd(userAddRequest);
        return TheResult.success(CodeBindMessage.SUCCESS, user);
    }

    /**
     * 删除用户网络接口
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> userDelete(@RequestBody UserDeleteRequest userDeleteRequest) {
        Boolean result = userService.userDelete(userDeleteRequest);
        return TheResult.success(CodeBindMessage.SUCCESS, result);
    }

    /**
     * 修改用户网络接口
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @PostMapping("/update")
    public BaseResponse<User> userUpdate(@RequestBody UserUpdateRequest userUpdateRequest) {
        User user = userService.userUpdate(userUpdateRequest);
        return TheResult.success(CodeBindMessage.SUCCESS, user);
    }

    /**
     * 查询用户网络接口
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @PostMapping("/search")
    public BaseResponse<List<User>> userSearch(@RequestBody UserSearchRequest userSearchRequest) {
        List<User> userList = userService.userSearch(userSearchRequest);
        return TheResult.success(CodeBindMessage.SUCCESS, userList);
    }

    /**
     * 封禁用户网络接口
     */
    @SaCheckLogin
    @SaCheckRole("admin")
    @PostMapping("/disable")
    public BaseResponse<Boolean> userDisable(@RequestBody UserDisableRequest userDisableRequest) {
        Boolean result = userService.userDisable(userDisableRequest.getId(), userDisableRequest.getDisableTime());
        return TheResult.success(CodeBindMessage.SUCCESS, result);
    }

    /**
     * 用户注册网络接口
     */
    @SaIgnore
    @PostMapping("/register")
    public BaseResponse<Boolean> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        Boolean result = userService.userRegister(userRegisterRequest.getAccount(), userRegisterRequest.getPasswd(), userRegisterRequest.getCheckPasswd());
        return TheResult.success(CodeBindMessage.SUCCESS, result);
    }

    /**
     * 用户登入网络接口
     */
    @SaIgnore
    @PostMapping("/login")
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        User user = userService.userLogin(userLoginRequest.getAccount(), userLoginRequest.getPasswd(), DeviceUtil.getRequestDevice(request)); // 这里同时解析用户的设备, 以支持同端互斥
        UserVO userVo = UserVO.removeSensitiveData(user);
        return TheResult.success(CodeBindMessage.SUCCESS, userVo);
    }

    /**
     * 用户登出网络接口
     */
    @SaCheckLogin
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        Boolean result = userService.userLogout(DeviceUtil.getRequestDevice(request));
        return TheResult.success(CodeBindMessage.SUCCESS, result);
    }

    /**
     * 获取状态网络接口
     */
    @SaIgnore
    @GetMapping("/status")
    public BaseResponse<UserDto> userStatus() {
        UserDto userDto = userService.userStatus();
        return TheResult.success(CodeBindMessage.SUCCESS, userDto);
    }

}
