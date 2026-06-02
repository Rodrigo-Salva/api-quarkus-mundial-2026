package com.mundial2026.predictions.shared.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

/**
 * Servicio de almacenamiento de archivos.
 * En desarrollo usa MinIO (puerto 9000, compatible S3).
 * En producción usa AWS S3 — solo cambia el endpoint en .env.
 *
 * Buckets:
 *  - mundial-avatars  → fotos de perfil de usuarios
 *  - mundial-leagues  → imágenes de salas
 *  - mundial-exports  → PDFs exportados
 */
@ApplicationScoped
public class StorageService {

    private static final Logger LOG = Logger.getLogger(StorageService.class);

    public static final String BUCKET_AVATARS = "mundial-avatars";
    public static final String BUCKET_LEAGUES = "mundial-leagues";
    public static final String BUCKET_EXPORTS = "mundial-exports";

    @ConfigProperty(name = "storage.public-url", defaultValue = "http://localhost:9000")
    String publicUrl;

    @Inject
    S3Client s3;

    /**
     * Sube un archivo y retorna la URL pública.
     *
     * @param bucket     bucket destino (usa las constantes BUCKET_*)
     * @param prefix     carpeta dentro del bucket (ej: "avatars/", "leagues/")
     * @param bytes      contenido del archivo
     * @param contentType MIME type (ej: "image/jpeg", "application/pdf")
     * @return URL pública del archivo subido
     */
    public String upload(String bucket, String prefix,
                         byte[] bytes, String contentType) {
        String key = prefix + UUID.randomUUID() + extensionFor(contentType);

        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength((long) bytes.length)
                        .build(),
                RequestBody.fromBytes(bytes));

        String url = publicUrl + "/" + bucket + "/" + key;
        LOG.infof("Archivo subido: %s", url);
        return url;
    }

    /**
     * Elimina un archivo por su URL pública.
     */
    public void delete(String publicFileUrl) {
        try {
            // Extrae bucket y key de la URL
            String path = publicFileUrl.replace(publicUrl + "/", "");
            int slash = path.indexOf('/');
            if (slash == -1) return;
            String bucket = path.substring(0, slash);
            String key    = path.substring(slash + 1);
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            LOG.warnf("No se pudo eliminar el archivo: %s — %s", publicFileUrl, e.getMessage());
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg"       -> ".jpg";
            case "image/png"        -> ".png";
            case "image/webp"       -> ".webp";
            case "application/pdf"  -> ".pdf";
            default                 -> "";
        };
    }
}
