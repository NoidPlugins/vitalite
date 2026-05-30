package com.tonic.bootstrap;

import com.google.gson.Gson;
import com.tonic.Static;
import com.tonic.bootstrap.beans.Artifact;
import com.tonic.bootstrap.beans.Bootstrap;
import com.tonic.bootstrap.beans.Platform;
import com.tonic.util.HashUtil;
import com.tonic.util.LauncherVersionUtil;
import com.tonic.vitalite.Main;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static com.tonic.vitalite.Main.REPOSITORY_DIR;

public class RLUpdater
{
    private static Map<String, String> properties;
    private static HttpClient httpClient;

    public static void main(String[] args) throws Exception
    {
        run();
    }

    public static void run() throws IOException, InterruptedException, NoSuchAlgorithmException
    {
        properties = new HashMap<>(); //Properties.fetch();
        properties.put("runelite.launcher.version", LauncherVersionUtil.getLauncherVersion());

        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest bootstrapReq = HttpRequest.newBuilder()
                .uri(URI.create("https://static.runelite.net/bootstrap.json"))
                .header("User-Agent", "RuneLite/" + properties.get("runelite.launcher.version"))
                .GET()
                .build();

        HttpResponse<String> bootstrapRes = httpClient.send(bootstrapReq,
                HttpResponse.BodyHandlers.ofString());

        if (bootstrapRes.statusCode() != 200) {
            throw new IOException("Failed to fetch bootstrap JSON (status=" + bootstrapRes.statusCode() + ")");
        }

        Bootstrap bootstrap = new Gson().fromJson(bootstrapRes.body(), Bootstrap.class);
        Artifact[] artifacts = bootstrap.getArtifacts();

        if (!Files.exists(REPOSITORY_DIR)) {
            Files.createDirectories(REPOSITORY_DIR);
        }

        // Phase 1: Check if any artifact needs updating
        boolean needsUpdate = false;
        String forcedVersion = Static.getCliArgs().getTargetBootstrap();
        String version = bootstrap.getVersion();
        boolean forceArtifactVersion = forcedVersion != null && !forcedVersion.isEmpty();
        boolean forcedVersionMatchesBootstrap = forceArtifactVersion && forcedVersion.equals(version);

        for (Artifact art : artifacts) {
            if (!platformMatches(art)) {
                continue;
            }

            String artName = resolveArtifactName(art, version, forcedVersion);
            Path localFile = REPOSITORY_DIR.resolve(artName);

            if (!Files.exists(localFile)) {
                needsUpdate = true;
                System.out.println("Missing artifact: " + artName);
                break;
            }

            if (!forceArtifactVersion || forcedVersionMatchesBootstrap) {
                String localHash = HashUtil.computeSha256(localFile);
                if (!localHash.equalsIgnoreCase(art.getHash())) {
                    needsUpdate = true;
                    System.out.println("Hash mismatch for " + artName);
                    break;
                }
            }
        }

        // Phase 2: If update needed, nuke repository and re-download everything
        if (needsUpdate) {
            System.out.println("Updates detected, cleaning repository...");

            // Delete all files in repository directory
            if (Files.exists(REPOSITORY_DIR)) {
                Files.list(REPOSITORY_DIR).forEach(file -> {
                    try {
                        Files.delete(file);
                        System.out.println("Deleted: " + file.getFileName());
                    } catch (IOException e) {
                        System.err.println("Failed to delete " + file.getFileName() + ": " + e.getMessage());
                    }
                });
            }

            // Download all artifacts fresh
            for (Artifact art : artifacts) {
                // Skip artifacts that don't match current platform
                if (!platformMatches(art)) {
                    System.out.println("Skipping " + art.getName() + " (platform mismatch)");
                    continue;
                }

                String artName = resolveArtifactName(art, version, forcedVersion);
                String path = resolveArtifactPath(art, version, forcedVersion);
                if(forceArtifactVersion)
                {
                    System.out.println("Forcing version " + forcedVersion + " for artifact " + artName);
                }

                Path localFile = REPOSITORY_DIR.resolve(artName);

                System.out.println("Downloading " + artName);
                downloadFile(path, localFile);

                String downloadedHash = HashUtil.computeSha256(localFile);
                if ((!forceArtifactVersion || forcedVersionMatchesBootstrap) && !downloadedHash.equalsIgnoreCase(art.getHash())) {
                    throw new IOException("Hash mismatch for " + artName
                            + " (expected " + art.getHash()
                            + ", got " + downloadedHash + ")");
                }
            }

            System.out.println("Repository updated successfully!");
        } else {
            System.out.println("Repository is up to date!");
        }
    }

    private static String resolveArtifactName(Artifact artifact, String bootstrapVersion, String forcedVersion)
    {
        return replaceBootstrapVersion(artifact.getName(), bootstrapVersion, forcedVersion);
    }

    private static String resolveArtifactPath(Artifact artifact, String bootstrapVersion, String forcedVersion)
    {
        return replaceBootstrapVersion(artifact.getPath(), bootstrapVersion, forcedVersion);
    }

    private static String replaceBootstrapVersion(String value, String bootstrapVersion, String forcedVersion)
    {
        if (forcedVersion == null || forcedVersion.isEmpty() || bootstrapVersion == null || bootstrapVersion.isEmpty())
        {
            return value;
        }
        return value.replace(bootstrapVersion, forcedVersion);
    }

    private static void downloadFile(String url, Path destination)
            throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "RuneLite/" + properties.get("runelite.launcher.version"))
                .GET()
                .build();

        HttpResponse<InputStream> res = httpClient.send(req,
                HttpResponse.BodyHandlers.ofInputStream());

        if (res.statusCode() != 200) {
            throw new IOException("Failed to download " + url
                    + " (status=" + res.statusCode() + ")");
        }

        try (InputStream in = res.body();
             OutputStream out = Files.newOutputStream(destination)) {
            in.transferTo(out);
        }
    }

    private static String getCurrentOS()
    {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "win";
        } else if (osName.contains("mac")) {
            return "macos";
        } else if (osName.contains("linux")) {
            return "linux";
        }
        return "unknown";
    }

    private static String getCurrentArch()
    {
        String osArch = System.getProperty("os.arch").toLowerCase();
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            return "x64";
        } else if (osArch.contains("x86")) {
            return "x86";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            return "aarch64";
        }
        return "unknown";
    }

    private static boolean platformMatches(Artifact artifact)
    {
        Platform[] platforms = artifact.getPlatform();

        // If no platform restrictions, artifact applies to all platforms
        if (platforms == null || platforms.length == 0) {
            return true;
        }

        String currentOS = getCurrentOS();
        String currentArch = getCurrentArch();

        // Check if any platform restriction matches current platform
        for (Platform platform : platforms) {
            boolean osMatches = platform.getName() == null || platform.getName().equalsIgnoreCase(currentOS);
            boolean archMatches = platform.getArch() == null || platform.getArch().equalsIgnoreCase(currentArch);

            if (osMatches && archMatches) {
                return true;
            }
        }

        return false;
    }
}
