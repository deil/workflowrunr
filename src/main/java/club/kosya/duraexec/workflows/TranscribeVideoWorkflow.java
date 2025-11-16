package club.kosya.duraexec.workflows;

import club.kosya.lib.workflow.ExecutionContext;
import club.kosya.duraexec.internal.ExecutionResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static club.kosya.lib.executionengine.ExecutionContextImpl.executeProcess;

@Slf4j
@Component
public class TranscribeVideoWorkflow {
    @SneakyThrows
    public String processVideo(ExecutionContext ctx, String videoFile) {
        log.info("processVideo(ctx={}, videoFile={})", ctx, videoFile);

        var videoPath = Paths.get(System.getProperty("user.dir")).resolve(videoFile);
        if (!Files.exists(videoPath)) {
            throw new IllegalArgumentException("Video file does not exist: " + videoFile);
        }

        ctx.sleep(Duration.ofSeconds(5));

        var audioFile = ctx.await("Extract audio track", () -> extractAudio(videoPath));
        try {
            return ctx.await("Transcribe audio to text", () -> transcribeAudio(audioFile));
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }

    private Path extractAudio(Path videoFile) {
        var videoFileName = videoFile.getFileName().toString();
        var audioFileName = videoFileName.replaceFirst("\\.[^.]+$", ".wav");
        var audioFile = videoFile.getParent().resolve(audioFileName);

        var result = executeProcess("ffmpeg",
                "-i", videoFile.toString(),
                "-vn", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                "-y", audioFile.toString());

        if (!result.isSuccess()) {
            throw new RuntimeException("FFmpeg failed with exit code: " + result.getExitCode());
        }

        return audioFile;
    }

    private String transcribeAudio(Path audioFile) {
        ExecutionResult result = executeProcess(
                Paths.get(System.getProperty("user.dir"), "whisper").toFile(),
                "uv", "run", "whisper", audioFile.toString(), "--model", "base");

        if (!result.isSuccess()) {
            throw new RuntimeException("Whisper transcription failed with exit code: " + result.getExitCode());
        }

        return result.getOutput();
    }
}
