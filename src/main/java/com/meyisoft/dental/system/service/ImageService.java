package com.meyisoft.dental.system.service;

import com.meyisoft.dental.system.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Servicio para procesar imágenes antes de subirlas al almacenamiento.
 * Implementa un "algoritmo de compresión" para reducir el peso y redimensionar.
 */
@Slf4j
@Service
public class ImageService {

    /**
     * Comprime y redimensiona una imagen.
     * 
     * @param file Archivo original
     * @param maxWidth Ancho máximo permitido
     * @param quality Calidad de compresión (0.0 a 1.0)
     * @return Arreglo de bytes con la imagen procesada
     */
    public byte[] compressImage(MultipartFile file, int maxWidth, float quality) {
        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new BusinessException("INVALID_IMAGE", "El archivo no es una imagen válida", HttpStatus.BAD_REQUEST);
            }

            // 1. Calcular nuevas dimensiones manteniendo el aspect ratio
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            if (width > maxWidth) {
                height = (height * maxWidth) / width;
                width = maxWidth;
            }

            // 2. Redimensionar
            Image resizedImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(resizedImage, 0, 0, null);
            g2d.dispose();

            // 3. Comprimir (JPEG)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) throw new IOException("No hay escritores JPEG disponibles");

            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(quality);
                }
                
                writer.write(null, new IIOImage(outputImage, null, null), param);
            }
            writer.dispose();

            log.info("Imagen comprimida: {}x{} (Calidad: {})", width, height, quality);
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Error al comprimir imagen: {}", e.getMessage());
            throw new BusinessException("COMPRESSION_ERROR", "No se pudo procesar la imagen", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Helper para convertir bytes a un MultipartFile falso o simplemente usarlos directamente con R2.
     * Como StorageService usa RequestBody.fromInputStream, podemos pasar un ByteArrayInputStream.
     */
}
