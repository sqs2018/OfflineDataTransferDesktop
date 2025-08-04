package com.sqs;


import com.google.zxing.WriterException;
import com.sqs.util.ConfigReader;
import com.sqs.util.FileSplitter;
import com.sqs.util.FileSplitterInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class APP extends JFrame {
    private JLabel qrImageLabel;
    private JTextField filePathField;
    private JTextArea logArea;
    private long startIndex=1;
    java.util.Timer timer=null;
    FileSplitterInfo fileSplitterInfo=null;

    private JTextField jumpField;
    //默认一秒生成一个二维码
    String interval="1000";
    public APP() {
        setTitle("基于二维码离线文件传输工具");
        setSize(650, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout
        setLayout(new BorderLayout());

        // Top Panel: File Selection
        JPanel topPanel = new JPanel(new BorderLayout());
        filePathField = new JTextField();
        filePathField.setEditable(false);
        JButton selectFileButton = new JButton("选择文件");
        selectFileButton.addActionListener(e -> selectFile());
        topPanel.add(filePathField, BorderLayout.CENTER);
        topPanel.add(selectFileButton, BorderLayout.EAST);

        // Middle Panel: QR Code Display
        JPanel middlePanel = new JPanel(new BorderLayout());
        qrImageLabel = new JLabel("支持文件拖入", SwingConstants.CENTER);
        qrImageLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        middlePanel.add(qrImageLabel, BorderLayout.CENTER);

        // Bottom Panel: Log Area and Generate Button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        logArea = new JTextArea(5, 50);
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        JButton generateButton = new JButton("生成二维码");
        generateButton.addActionListener(e -> {
            try {
                generateQRCode();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Add the jump field and button
        JPanel jumpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jumpField = new JTextField(5); // Input field for the jump index
        JButton jumpButton = new JButton("跳转");
        jumpButton.addActionListener(e -> {
            try {
                int jumpIndex = Integer.parseInt(jumpField.getText());
                if (jumpIndex > 0 && jumpIndex <= fileSplitterInfo.getSplitLength()) {
                    startIndex = jumpIndex;
                    FileInputStream fileInputStream = new FileInputStream(filePathField.getText());
                    fileInputStream.skip((long) (FileSplitter.DEFAULT_SPLIT_LENGTH * (jumpIndex - 1))); // Skip to the correct position
                    byte[] buf = new byte[FileSplitter.DEFAULT_SPLIT_LENGTH];
                    int bytesRead = fileInputStream.read(buf);
                    if(bytesRead!=-1){
                        ImageIcon qrCodeImage = FileSplitter.generateBinaryQRCode(jumpIndex,buf);
                        qrImageLabel.setIcon(qrCodeImage);
                    }
                    fileInputStream.close();
                    logArea.append("跳转到第 " + jumpIndex + " 个二维码.\n");
                } else {
                    logArea.append("跳转索引无效，请输入 1 到 " + fileSplitterInfo.getSplitLength() + " 之间的数字.\n");
                }
            } catch (Exception ex) {
                logArea.append("请输入有效的数字.\n");
            }
        });
        jumpPanel.add(new JLabel("跳转到:"));
        jumpPanel.add(jumpField);
        jumpPanel.add(jumpButton);

        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(jumpPanel, BorderLayout.NORTH);
        bottomPanel.add(generateButton, BorderLayout.SOUTH);

        // Add Panels to Frame
        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);


        // Add drag-and-drop functionality
        new DropTarget(this, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {}

            @Override
            public void dragOver(DropTargetDragEvent dtde) {}

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {}

            @Override
            public void dragExit(DropTargetEvent dte) {}

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        java.util.List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            File droppedFile = files.get(0);
                            filePathField.setText(droppedFile.getAbsolutePath());
                            logArea.append("文件已拖入: " + droppedFile.getAbsolutePath() + "\n");
                        }
                    }
                    qrImageLabel.setText("");
                } catch (Exception ex) {
                    logArea.append("拖入文件时发生错误: " + ex.getMessage() + "\n");
                }
            }
        });
        //读取配置文件
        ConfigReader configReader = null;
        try {
            configReader = new ConfigReader("config.inf");
            interval = configReader.getProperty("INTERVAL");
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
        }


    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }
    FileInputStream fileInputStream=null;
    private void generateQRCode() throws IOException {
        String filePath = filePathField.getText();
        if (filePath.isEmpty()) {
            logArea.append("请选择文件.\n");
            return;
        }
        if(timer!=null) {
            timer.cancel(); // Stop the previous timer if it exists
        }
        qrImageLabel.setText("");
        startIndex=1;
        fileSplitterInfo= FileSplitter.getFileSplitterInfo(new File(filePath));
        logArea.append("分析文件成功: ");
        logArea.append("文件名: " + fileSplitterInfo.getFileName() + ", ");
        logArea.append("文件大小: " + fileSplitterInfo.getFileSize() + " 字节, ");
        logArea.append("二维码: " + fileSplitterInfo.getSplitLength() + " 个.\n");
        Timer timerStart= new Timer();
        TimerTask taskStart = new TimerTask() {
            @Override
            public void run() {
                //显示文件列表
                try {
                    ImageIcon qrCodeImage = FileSplitter.generateBinaryQRCode(-1,(fileSplitterInfo.getFileName()+":"+fileSplitterInfo.getSplitLength()).getBytes(StandardCharsets.UTF_8));
                    qrImageLabel.setIcon(qrCodeImage);
                    logArea.append("显示文件信息.\n");
                    timerStart.cancel(); // Stop the timer
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };
        timerStart.scheduleAtFixedRate(taskStart, 0, 100); // Schedule task every 1 second
        timer= new Timer();
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        byte[] buf = new byte[FileSplitter.DEFAULT_SPLIT_LENGTH];
                        int bytesRead = fileInputStream.read(buf);
                        if (bytesRead <= 0) {
                            logArea.append("文件读取完毕.\n");
                            fileInputStream.close();

                            //显示结束二维码
                            ImageIcon qrCodeImage = FileSplitter.generateBinaryQRCode(-2,(fileSplitterInfo.getFileName()+":"+fileSplitterInfo.getSplitLength()).getBytes(StandardCharsets.UTF_8));
                            qrImageLabel.setIcon(qrCodeImage);


                            timer.cancel(); // Stop the timer
                            return;
                        }
                        if(startIndex%10==0){
                            logArea.setText("");
                        }
                        ImageIcon qrCodeImage = FileSplitter.generateBinaryQRCode(startIndex,buf);
                        qrImageLabel.setIcon(qrCodeImage);
                        logArea.append("正在显示第"+startIndex+"个二维码，一共" + fileSplitterInfo.getSplitLength() + " 个.\n");
                        startIndex++;
                    } catch (Exception ex) {
                        logArea.append("生成二维码错误: " + ex.getMessage() + "\n");
                        try {
                            fileInputStream.close();
                        } catch (IOException ioException) {
                            logArea.append("Error closing file: " + ioException.getMessage() + "\n");
                        }
                        timer.cancel(); // Stop the timer
                    }
                }
            };
            timer.scheduleAtFixedRate(task, 5000, Integer.valueOf(interval)); // Schedule task every 1 second



        } catch (Exception e) {
            logArea.append("Error reading file: " + e.getMessage() + "\n");
        }finally {
            if(fileInputStream!=null)
                fileInputStream.close();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            APP gui = new APP();
            gui.setVisible(true);
        });
    }
}