package com.mundial2026.predictions.shared.storage;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mundial2026.predictions.auth.repository.UserRepository;
import com.mundial2026.predictions.leagues.dto.LeagueRankingEntry;
import com.mundial2026.predictions.leagues.entity.League;
import com.mundial2026.predictions.leagues.repository.LeagueRepository;
import com.mundial2026.predictions.leagues.service.LeagueService;
import com.mundial2026.predictions.shared.response.ApiResponse;
import com.mundial2026.predictions.shared.security.SecurityUtils;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Path("/api/storage")
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Storage", description = "Subida de archivos — fotos de perfil, imágenes de sala y exportación de rankings.")
public class StorageResource {

    @Inject StorageService  storageService;
    @Inject SecurityUtils   securityUtils;
    @Inject UserRepository  userRepository;
    @Inject LeagueRepository leagueRepository;
    @Inject LeagueService   leagueService;

    // ── Avatar del usuario ────────────────────────────────────────

    @PUT
    @Path("/avatar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Subir foto de perfil",
               description = "Sube una imagen como avatar del usuario. Formatos: JPG, PNG, WEBP. Máximo 5MB.")
    @APIResponse(responseCode = "200", description = "URL pública del avatar guardada en el perfil.")
    @Transactional
    public Response uploadAvatar(@FormParam("file") FileUpload file) {
        Long userId = securityUtils.getCurrentUserId();
        validateImage(file);

        byte[] bytes = readFile(file);
        String url   = storageService.upload(
                StorageService.BUCKET_AVATARS,
                "user-" + userId + "/",
                bytes, file.contentType());

        // Actualiza el campo en la BD
        userRepository.findByIdOptional(userId).ifPresent(user -> {
            if (user.avatarUrl != null) storageService.delete(user.avatarUrl);
            user.avatarUrl = url;
        });

        return Response.ok(ApiResponse.ok(url, "Foto de perfil actualizada")).build();
    }

    // ── Imagen de sala ────────────────────────────────────────────

    @PUT
    @Path("/leagues/{leagueId}/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Subir imagen de sala",
               description = "Solo el creador de la sala puede subir su imagen. Formatos: JPG, PNG, WEBP. Máximo 5MB.")
    @APIResponse(responseCode = "200", description = "URL pública de la imagen guardada.")
    @Transactional
    public Response uploadLeagueImage(
            @Parameter(description = "ID de la sala", required = true)
            @PathParam("leagueId") Long leagueId,
            @FormParam("file") FileUpload file) {

        Long userId = securityUtils.getCurrentUserId();
        validateImage(file);

        League league = leagueRepository.findById(leagueId);
        if (league == null) throw new NotFoundException("Sala no encontrada");
        if (!league.ownerId.equals(userId))
            throw new SecurityException("Solo el creador de la sala puede cambiar su imagen");

        byte[] bytes = readFile(file);
        String url   = storageService.upload(
                StorageService.BUCKET_LEAGUES,
                "league-" + leagueId + "/",
                bytes, file.contentType());

        if (league.imageUrl != null) storageService.delete(league.imageUrl);
        league.imageUrl = url;

        return Response.ok(ApiResponse.ok(url, "Imagen de sala actualizada")).build();
    }

    // ── Export PDF ranking sala ───────────────────────────────────

    @GET
    @Path("/leagues/{leagueId}/ranking/export")
    @Produces("application/pdf")
    @Operation(summary = "Exportar ranking de sala en PDF",
               description = "Genera y devuelve un PDF con el ranking actual de la sala.")
    @APIResponse(responseCode = "200", description = "Archivo PDF del ranking.")
    public Response exportRankingPdf(
            @Parameter(description = "ID de la sala", required = true)
            @PathParam("leagueId") Long leagueId) {

        League league = leagueRepository.findById(leagueId);
        if (league == null) throw new NotFoundException("Sala no encontrada");

        List<LeagueRankingEntry> ranking = leagueService.getLeagueRanking(leagueId, 100);
        byte[] pdf = generateRankingPdf(league.name, ranking);

        return Response.ok(pdf)
                .type("application/pdf")
                .header("Content-Disposition",
                        "attachment; filename=\"ranking-" + league.code + ".pdf\"")
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void validateImage(FileUpload file) {
        if (file == null || file.size() == 0)
            throw new IllegalArgumentException("No se recibió ningún archivo");

        String ct = file.contentType();
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(ct))
            throw new IllegalArgumentException(
                    "Formato no soportado. Usa JPG, PNG o WEBP");

        if (file.size() > 5 * 1024 * 1024)
            throw new IllegalArgumentException(
                    "El archivo supera el límite de 5MB");
    }

    private byte[] readFile(FileUpload file) {
        try {
            return java.nio.file.Files.readAllBytes(file.uploadedFile());
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo el archivo: " + e.getMessage(), e);
        }
    }

    private byte[] generateRankingPdf(String leagueName, List<LeagueRankingEntry> ranking) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Título
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.decode("#1a1a2e"));
            Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
            Font bodyFont   = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);

            Paragraph title = new Paragraph("🏆 Ranking — " + leagueName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            doc.add(title);

            Paragraph subtitle = new Paragraph(
                    "Generado: " + java.time.LocalDate.now(),
                    new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            doc.add(subtitle);

            // Tabla
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 3f, 2f, 2f});

            // Cabecera
            Color headerBg = Color.decode("#16213e");
            for (String col : new String[]{"#", "Usuario", "País", "Puntos"}) {
                PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Filas
            for (int i = 0; i < ranking.size(); i++) {
                LeagueRankingEntry e = ranking.get(i);
                Color rowBg = i % 2 == 0 ? Color.WHITE : Color.decode("#f0f0f0");

                PdfPCell posCell = new PdfPCell(new Phrase(String.valueOf(e.position()), bodyFont));
                posCell.setBackgroundColor(rowBg);
                posCell.setPadding(6);
                posCell.setHorizontalAlignment(Element.ALIGN_CENTER);

                PdfPCell nameCell = new PdfPCell(new Phrase(e.username(), bodyFont));
                nameCell.setBackgroundColor(rowBg);
                nameCell.setPadding(6);

                PdfPCell countryCell = new PdfPCell(new Phrase(e.country(), bodyFont));
                countryCell.setBackgroundColor(rowBg);
                countryCell.setPadding(6);
                countryCell.setHorizontalAlignment(Element.ALIGN_CENTER);

                Font pointsFont = new Font(Font.HELVETICA, 10, Font.BOLD,
                        i == 0 ? Color.decode("#f0a500") : Color.BLACK);
                PdfPCell ptsCell = new PdfPCell(new Phrase(e.points() + " pts", pointsFont));
                ptsCell.setBackgroundColor(rowBg);
                ptsCell.setPadding(6);
                ptsCell.setHorizontalAlignment(Element.ALIGN_CENTER);

                table.addCell(posCell);
                table.addCell(nameCell);
                table.addCell(countryCell);
                table.addCell(ptsCell);
            }

            doc.add(table);

            Paragraph footer = new Paragraph(
                    "\nMundial 2026 Predictions — mundial2026.com",
                    new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            doc.add(footer);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF: " + e.getMessage(), e);
        }
    }
}
