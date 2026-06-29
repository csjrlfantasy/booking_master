package com.ticket.app.controller.admin;

import com.ticket.app.common.Result;
import com.ticket.app.common.ResultCode;
import com.ticket.app.common.exception.BusinessException;
import com.ticket.app.common.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin/files")
@RequiredArgsConstructor
public class AdminFileController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final JwtUtils jwtUtils;

    @PostMapping("/upload")
    public Result<Map<String, String>> uploadTicketImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long adminId = getAdminIdFromRequest(request);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请选择图片文件");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片只支持jpg、png、webp格式");
        }

        try {
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "tickets").toAbsolutePath();
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = uploadDir.resolve(filename);
            file.transferTo(target);

            String url = "/api/uploads/tickets/" + filename;
            log.info("管理员上传票务图片: adminId={}, file={}, url={}", adminId, originalName, url);
            return Result.success(Map.of("url", url));
        } catch (IOException e) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "图片上传失败");
        }
    }

    private String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private Long getAdminIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "请先登录");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            if (userId == null) {
                throw new BusinessException(ResultCode.TOKEN_INVALID, "Token解析失败");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.TOKEN_INVALID, "Token无效");
        }
    }
}
