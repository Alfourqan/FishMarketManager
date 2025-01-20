package com.poissonnerie.util;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.itextpdf.text.DocumentException;

public class PDFUtils {
    private static final Logger LOGGER = Logger.getLogger(PDFUtils.class.getName());

    public static void sauvegarderPDF(byte[] pdfData, String nomFichier) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(nomFichier)) {
            fos.write(pdfData);
            LOGGER.info("PDF sauvegardé avec succès : " + nomFichier);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la sauvegarde du PDF", e);
            throw e;
        }
    }

    public static byte[] getBytes(ByteArrayOutputStream baos) {
        return baos.toByteArray();
    }

    public static void convertirEtSauvegarder(ByteArrayOutputStream baos, String nomFichier) throws IOException {
        sauvegarderPDF(getBytes(baos), nomFichier);
    }
}