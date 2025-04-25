package cn.com.edtechhub.workmassivelikes.controller;

import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessage;
import cn.com.edtechhub.workmassivelikes.response.BaseResponse;
import cn.com.edtechhub.workmassivelikes.response.TheResult;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // 返回值默认为 json 类型
@RequestMapping("/thumb")
@Slf4j
public class ThumbController {

    @Resource
    private ThumbService thumbService;

    @SaCheckLogin
    @PostMapping("/add/do")
    public BaseResponse<Boolean> thumbAddDo(Long blogId) {
        return TheResult.success(CodeBindMessage.SUCCESS, thumbService.thumbAddDoUseRedis(blogId));
    }

    @SaCheckLogin
    @PostMapping("/add/undo")
    public BaseResponse<Boolean> thumbAddUnDo(Long blogId) {
        return TheResult.success(CodeBindMessage.SUCCESS, thumbService.thumbAddUnDoUseRedis(blogId));
    }

}
