package kkhura.com.uploadingimage;

public class UploadInfo {
    public String name;
    public String url;

    public UploadInfo(){

    }

    public UploadInfo(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

}
