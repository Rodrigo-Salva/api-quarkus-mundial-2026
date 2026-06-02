package com.mundial2026.predictions.leagues.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mundial2026.predictions.leagues.entity.League;
import com.mundial2026.predictions.leagues.repository.LeagueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@ApplicationScoped
public class InviteService {

    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:3000")
    String frontendUrl;

    @Inject
    LeagueRepository leagueRepository;

    /**
     * Devuelve la URL de invitación para una sala.
     * Formato: http://localhost:3000/join/{code}
     * El front intercepta /join/{code}, llama a POST /api/leagues/{code}/join
     */
    public String getInviteLink(Long leagueId) {
        League league = findLeague(leagueId);
        return frontendUrl + "/join/" + league.code;
    }

    /**
     * Genera una imagen PNG del QR con el link de invitación.
     * El front solo necesita: <img src="/api/leagues/{id}/invite/qr" />
     */
    public byte[] generateQrCode(Long leagueId, int size) {
        String inviteUrl = getInviteLink(leagueId);
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );

            var bitMatrix = writer.encode(inviteUrl, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y)
                            ? Color.BLACK.getRGB()
                            : Color.WHITE.getRGB());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando el código QR: " + e.getMessage(), e);
        }
    }

    private League findLeague(Long leagueId) {
        League league = leagueRepository.findById(leagueId);
        if (league == null) throw new NotFoundException("Sala no encontrada: " + leagueId);
        return league;
    }
}
