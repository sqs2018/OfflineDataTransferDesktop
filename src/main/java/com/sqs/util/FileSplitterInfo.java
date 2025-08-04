package com.sqs.util;


/***
 * 文件切分实体类
 */
public class FileSplitterInfo {
    private String fileName; // 文件名
    private long fileSize; // 文件大小
    private long splitLength; // 切分长度

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getSplitLength() {
        return splitLength;
    }

    public void setSplitLength(long splitLength) {
        this.splitLength = splitLength;
    }
}
