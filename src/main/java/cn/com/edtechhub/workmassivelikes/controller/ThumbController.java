package cn.com.edtechhub.workmassivelikes.controller;

import cn.com.edtechhub.workmassivelikes.enums.CodeBindMessageEnum;
import cn.com.edtechhub.workmassivelikes.request.ThumbDoRequest;
import cn.com.edtechhub.workmassivelikes.request.ThumbUnDoRequest;
import cn.com.edtechhub.workmassivelikes.response.BaseResponse;
import cn.com.edtechhub.workmassivelikes.response.TheResult;
import cn.com.edtechhub.workmassivelikes.service.ThumbService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public BaseResponse<Boolean> thumbDo(@RequestBody ThumbDoRequest thumbDoRequest) {
        return TheResult.success(CodeBindMessageEnum.SUCCESS, thumbService.thumbAddDoUseMQ(thumbDoRequest.getBlogId()));
    }

    @SaCheckLogin
    @PostMapping("/add/undo")
    public BaseResponse<Boolean> thumbUnDo(@RequestBody ThumbUnDoRequest thumbUnDoRequest) {
        return TheResult.success(CodeBindMessageEnum.SUCCESS, thumbService.thumbAddUnDoUseMQ(thumbUnDoRequest.getBlogId()));
    }

}
