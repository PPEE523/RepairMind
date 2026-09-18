package com.example.equipment.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频关键帧提取工具
 * Windows 本地调试: 使用 JavaCV (javacv + ffmpeg-platform 提供原生库)
 * Linux 龙芯服务器: 使用系统 ffmpeg/ffprobe 命令行
 */
@Slf4j
public class VideoFrameExtractor {

    /** 默认提取帧数 */
    private static final int DEFAULT_FRAME_COUNT = 4;

    /** 输出图片最大宽度 */
    private static final int MAX_WIDTH = 1280;

    /** ffmpeg 命令超时（秒） */
    private static final int COMMAND_TIMEOUT = 60;

    /** 匹配 ffprobe 输出的时长（秒） */
    private static final Pattern DURATION_PATTERN = Pattern.compile("\"duration\"\\s*:\\s*\"([\\d.]+)\"");

    /** 是否为 Windows 平台 */
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    /** Linux 下 ffmpeg/ffprobe 绝对路径，避免 Java 进程 PATH 不一致 */
    private static final String FFMPEG_PATH = "/usr/bin/ffmpeg";
    private static final String FFPROBE_PATH = "/usr/bin/ffprobe";

    /**
     * 从视频文件提取关键帧并保存到本地
     *
     * @param videoFilePath 视频文件的绝对路径
     * @param uploadDir     上传根目录 (upload.dir)
     * @return 提取成功的帧图片相对URL列表（如 /uploads/videos/frames/2026/07/02/xxx_frame_1.jpg）
     */
    public static List<String> extractFrames(String videoFilePath, String uploadDir) {
        return extractFrames(videoFilePath, uploadDir, DEFAULT_FRAME_COUNT);
    }

    /**
     * 从视频文件提取指定数量的关键帧并保存到本地
     *
     * @param videoFilePath 视频文件的绝对路径
     * @param uploadDir     上传根目录 (upload.dir)
     * @param frameCount    期望提取的帧数
     * @return 提取成功的帧图片相对URL列表
     */
    public static List<String> extractFrames(String videoFilePath, String uploadDir, int frameCount) {
        if (IS_WINDOWS) {
            return extractFramesByJavaCV(videoFilePath, uploadDir, frameCount);
        } else {
            return extractFramesByFFmpeg(videoFilePath, uploadDir, frameCount);
        }
    }

    // ==================== Linux: 系统 ffmpeg ====================

    private static List<String> extractFramesByFFmpeg(String videoFilePath, String uploadDir, int frameCount) {
        List<String> frameUrls = new ArrayList<>();
        Path videoPath = Paths.get(videoFilePath);

        if (!Files.exists(videoPath)) {
            log.warn("[VideoFrameExtractor] 视频文件不存在: {}", videoFilePath);
            return frameUrls;
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path basePath = Paths.get(uploadDir).toAbsolutePath();
        Path frameDir = basePath.resolve("videos/frames").resolve(dateDir);

        try {
            if (!Files.exists(frameDir)) {
                Files.createDirectories(frameDir);
            }
        } catch (IOException e) {
            log.error("[VideoFrameExtractor] 创建帧输出目录失败: {}", frameDir, e);
            return frameUrls;
        }

        String videoFileName = videoPath.getFileName().toString();
        String namePrefix = videoFileName.contains(".")
                ? videoFileName.substring(0, videoFileName.lastIndexOf("."))
                : videoFileName;

        // 用 ffprobe 获取视频时长
        double duration = getVideoDuration(videoPath.toFile().getAbsolutePath());
        if (duration <= 0) {
            log.warn("[VideoFrameExtractor] 无法获取视频时长: {}", videoFileName);
            return frameUrls;
        }

        log.info("[VideoFrameExtractor][FFmpeg] 视频信息: file={}, duration={}s",
                videoFileName, String.format("%.2f", duration));

        int actualFrameCount = Math.min(frameCount, Math.max(1, (int) duration));

        for (int i = 0; i < actualFrameCount; i++) {
            double seekTime = ((double) (i + 1) / (actualFrameCount + 1)) * duration;
            String frameFileName = namePrefix + "_frame_" + (i + 1) + ".jpg";
            Path frameFile = frameDir.resolve(frameFileName);

            try {
                boolean success = extractFrameAt(
                        videoPath.toFile().getAbsolutePath(),
                        frameFile.toAbsolutePath().toString(),
                        seekTime
                );

                if (success && Files.exists(frameFile) && Files.size(frameFile) > 0) {
                    BufferedImage img = ImageIO.read(frameFile.toFile());
                    if (img != null) {
                        img = resizeImage(img, MAX_WIDTH);
                        ImageIO.write(img, "jpg", frameFile.toFile());
                    }

                    String frameUrl = "/uploads/videos/frames/" + dateDir + "/" + frameFileName;
                    frameUrls.add(frameUrl);
                    log.debug("[VideoFrameExtractor] 已提取第{}帧 (t={}s): {}", i + 1, String.format("%.2f", seekTime), frameUrl);
                } else {
                    log.warn("[VideoFrameExtractor] 第{}帧提取失败 (t={}s)", i + 1, String.format("%.2f", seekTime));
                }
            } catch (Exception e) {
                log.warn("[VideoFrameExtractor] 第{}帧提取异常: {}", i + 1, e.getMessage());
            }
        }

        log.info("[VideoFrameExtractor] 完成提取: file={}, 成功={} 帧", videoFileName, frameUrls.size());
        return frameUrls;
    }

    private static double getVideoDuration(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    FFPROBE_PATH, "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    videoPath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(COMMAND_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return -1;
            }

            if (process.exitValue() != 0) {
                return -1;
            }

            Matcher matcher = DURATION_PATTERN.matcher(output.toString());
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            log.error("[VideoFrameExtractor] ffprobe 调用失败: {}", e.getMessage(), e);
        }
        return -1;
    }

    private static boolean extractFrameAt(String inputPath, String outputPath, double timeSec) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    FFMPEG_PATH, "-y",
                    "-ss", String.format("%.3f", timeSec),
                    "-i", inputPath,
                    "-frames:v", "1",
                    "-q:v", "2",
                    outputPath
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                while (reader.readLine() != null) { /* drain */ }
            }

            boolean finished = process.waitFor(COMMAND_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("[VideoFrameExtractor] ffmpeg 提取帧异常: {}", e.getMessage());
            return false;
        }
    }

    // ==================== Windows: JavaCV ====================

    @SuppressWarnings("unchecked")
    private static List<String> extractFramesByJavaCV(String videoFilePath, String uploadDir, int frameCount) {
        List<String> frameUrls = new ArrayList<>();
        Path videoPath = Paths.get(videoFilePath);

        if (!Files.exists(videoPath)) {
            log.warn("[VideoFrameExtractor] 视频文件不存在: {}", videoFilePath);
            return frameUrls;
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path basePath = Paths.get(uploadDir).toAbsolutePath();
        Path frameDir = basePath.resolve("videos/frames").resolve(dateDir);

        try {
            if (!Files.exists(frameDir)) {
                Files.createDirectories(frameDir);
            }
        } catch (IOException e) {
            log.error("[VideoFrameExtractor] 创建帧输出目录失败: {}", frameDir, e);
            return frameUrls;
        }

        String videoFileName = videoPath.getFileName().toString();
        String namePrefix = videoFileName.contains(".")
                ? videoFileName.substring(0, videoFileName.lastIndexOf("."))
                : videoFileName;

        try {
            // 通过反射加载 JavaCV 类，避免在 Linux 龙芯上触发类加载失败
            Class<?> grabberClass = Class.forName("org.bytedeco.javacv.FFmpegFrameGrabber");
            Class<?> converterClass = Class.forName("org.bytedeco.javacv.Java2DFrameConverter");
            Class<?> frameClass = Class.forName("org.bytedeco.javacv.Frame");

            Object grabber = grabberClass.getConstructor(File.class).newInstance(videoPath.toFile());
            Object converter = converterClass.getConstructor().newInstance();

            try {
                grabberClass.getMethod("start").invoke(grabber);

                int totalFrames = (int) grabberClass.getMethod("getLengthInFrames").invoke(grabber);
                double frameRate = (double) grabberClass.getMethod("getFrameRate").invoke(grabber);
                double duration = totalFrames > 0 && frameRate > 0 ? totalFrames / frameRate : 0;

                log.info("[VideoFrameExtractor][JavaCV] 视频信息: file={}, totalFrames={}, frameRate={}, duration={}s",
                        videoFileName, totalFrames, frameRate, String.format("%.2f", duration));

                if (totalFrames <= 0) {
                    log.warn("[VideoFrameExtractor] 无法获取视频帧数，尝试基于时间的提取");
                    return extractByTimeJavaCV(grabber, converter, grabberClass, converterClass,
                            frameDir, namePrefix, dateDir, frameCount, duration, uploadDir, frameClass);
                }

                int actualFrameCount = Math.min(frameCount, Math.max(1, totalFrames));

                for (int i = 0; i < actualFrameCount; i++) {
                    int targetFrame = (int) ((double) (i + 1) / (actualFrameCount + 1) * totalFrames);
                    targetFrame = Math.max(0, Math.min(targetFrame, totalFrames - 1));

                    String frameFileName = namePrefix + "_frame_" + (i + 1) + ".jpg";
                    Path frameFile = frameDir.resolve(frameFileName);

                    try {
                        grabberClass.getMethod("setFrameNumber", int.class).invoke(grabber, targetFrame);
                        Object frame = grabberClass.getMethod("grabImage").invoke(grabber);

                        if (frame != null) {
                            BufferedImage bufferedImage = (BufferedImage) converterClass
                                    .getMethod("getBufferedImage", frameClass)
                                    .invoke(converter, frame);

                            if (bufferedImage != null) {
                                bufferedImage = resizeImage(bufferedImage, MAX_WIDTH);
                                ImageIO.write(bufferedImage, "jpg", frameFile.toFile());

                                if (Files.exists(frameFile) && Files.size(frameFile) > 0) {
                                    String frameUrl = "/uploads/videos/frames/" + dateDir + "/" + frameFileName;
                                    frameUrls.add(frameUrl);
                                    log.debug("[VideoFrameExtractor] 已提取第{}帧: {}", i + 1, frameUrl);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[VideoFrameExtractor] 第{}帧提取异常: {}", i + 1, e.getMessage());
                    }
                }
            } finally {
                grabberClass.getMethod("close").invoke(grabber);
            }

        } catch (Exception e) {
            log.error("[VideoFrameExtractor] JavaCV 视频处理失败: {}", e.getMessage(), e);
        }

        log.info("[VideoFrameExtractor] 完成提取: file={}, 成功={} 帧", videoFileName, frameUrls.size());
        return frameUrls;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractByTimeJavaCV(Object grabber, Object converter,
                                                     Class<?> grabberClass, Class<?> converterClass,
                                                     Path frameDir, String namePrefix, String dateDir,
                                                     int frameCount, double duration, String uploadDir,
                                                     Class<?> frameClass) {
        List<String> frameUrls = new ArrayList<>();

        if (duration <= 0) {
            return frameUrls;
        }

        try {
            double frameRate = (double) grabberClass.getMethod("getFrameRate").invoke(grabber);
            int actualCount = Math.min(frameCount, Math.max(1, (int) duration));

            for (int i = 0; i < actualCount; i++) {
                double seekTime = ((double) (i + 1) / (actualCount + 1)) * duration;
                String frameFileName = namePrefix + "_frame_" + (i + 1) + ".jpg";
                Path frameFile = frameDir.resolve(frameFileName);

                try {
                    int targetFrame = frameRate > 0 ? (int) (seekTime * frameRate) : (int) (seekTime * 30);
                    targetFrame = Math.max(0, targetFrame);

                    grabberClass.getMethod("setFrameNumber", int.class).invoke(grabber, targetFrame);
                    Object frame = grabberClass.getMethod("grabImage").invoke(grabber);

                    if (frame != null) {
                        BufferedImage bufferedImage = (BufferedImage) converterClass
                                .getMethod("getBufferedImage", frameClass)
                                .invoke(converter, frame);

                        if (bufferedImage != null) {
                            bufferedImage = resizeImage(bufferedImage, MAX_WIDTH);
                            ImageIO.write(bufferedImage, "jpg", frameFile.toFile());

                            if (Files.exists(frameFile) && Files.size(frameFile) > 0) {
                                String frameUrl = "/uploads/videos/frames/" + dateDir + "/" + frameFileName;
                                frameUrls.add(frameUrl);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("[VideoFrameExtractor] 时间提取第{}帧异常: {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[VideoFrameExtractor] JavaCV 时间提取失败", e);
        }

        return frameUrls;
    }

    // ==================== 公共工具 ====================

    private static BufferedImage resizeImage(BufferedImage source, int maxWidth) {
        int width = source.getWidth();
        int height = source.getHeight();

        if (width <= maxWidth || width <= 0 || height <= 0) {
            return source;
        }

        int newWidth = maxWidth;
        int newHeight = (int) ((double) height * maxWidth / width);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, newWidth, newHeight, null);
        } finally {
            g.dispose();
        }

        return resized;
    }
}
