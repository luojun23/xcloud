package com.njtech.xcloud.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

/**
 * 验证码生成工具类
 */
public class CaptchaUtil {

    // 验证码字符集
    private static final String CHAR_SET = "ABCDEFGHIJKLMNPQRSTUVWXYZabcdefghijklmnpqrstuvwxyz123456789";
    
    // 验证码长度
    private static final int CODE_LENGTH = 5;
    
    // 图片宽度
    private static final int WIDTH = 160;
    
    // 图片高度
    private static final int HEIGHT = 40;

    /**
     * 生成随机验证码
     * @return 验证码字符串
     */
    public static String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHAR_SET.charAt(random.nextInt(CHAR_SET.length())));
        }
        return code.toString();
    }

    /**
     * 生成验证码图片
     * @param code 验证码字符串
     * @return BufferedImage图片对象
     */
    public static BufferedImage generateImage(String code) {
        // 创建图片
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // 设置背景色
        g.setColor(getRandomColor(200, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制验证码字符
        g.setFont(getFont(24));
        
        // 绘制干扰线
        Random random = new Random();
        for (int i = 0; i < 20; i++) {
            g.setColor(getRandomColor(100, 200));
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        for (int i = 0; i < code.length(); i++) {
            g.setColor(getRandomColor(0, 100));
            // 添加旋转效果
            int x = 20 + i * 25;
            int y = 28;
            double angle = (random.nextDouble() - 0.5) * 0.3;
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.rotate(-angle, x, y);
        }
        
        // 绘制干扰点
        for (int i = 0; i < 50; i++) {
            g.setColor(getRandomColor(100, 200));
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            g.drawOval(x, y, 1, 1);
        }
        
        g.dispose();
        
        return image;
    }
    /**
     * @Description :  获取随机的字体
    * @Param :  * @param size
    * @Author : luojun
    * @Date : 2026/4/7 11:04
    * @return java.awt.Font
    */
    private static Font getFont(int size){
        Random random = new Random();
        Font font[] = {new Font("Times New Roman", Font.BOLD, size),
                new Font("Times New Roman", Font.ITALIC, size),
                new Font("Arial", Font.BOLD, size),
                new Font("Arial", Font.ITALIC, size),
                new Font("Courier New", Font.BOLD, size),
                new Font("Courier New", Font.ITALIC, size)};
        return font[random.nextInt(font.length)];
    }

    /**
     * 生成验证码图片的Base64编码
     * @param code 验证码字符串
     * @return Base64编码的图片
     */
    public static String generateImageBase64(String code) {
        BufferedImage image = generateImage(code);
        
        // 转换为Base64
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取随机颜色
     */
    private static Color getRandomColor(int fc, int bc) {
        Random random = new Random();
        if (fc > 255) fc = 255;
        if (bc > 255) bc = 255;
        int r = fc + random.nextInt(bc - fc);
        int g = fc + random.nextInt(bc - fc);
        int b = fc + random.nextInt(bc - fc);
        return new Color(r, g, b);
    }
}
