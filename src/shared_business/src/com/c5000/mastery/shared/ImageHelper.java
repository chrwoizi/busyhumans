package com.c5000.mastery.shared;

import com.c5000.mastery.shared.data.base.ResourceD;

public class ImageHelper {

    public static enum Size {
        SMALL,
        MEDIUM,
        LARGE,
        HIRES
    }

    public static String getUrl(ResourceD resource, Size size) {
        ResourceD imageD = resource;
        if(size == null)
            return imageD.resource;
        switch (size) {
            case SMALL:
                return imageD.small != null ? imageD.small : imageD.resource;
            case MEDIUM:
                return imageD.medium != null ? imageD.medium : imageD.resource;
            case LARGE:
                return imageD.large != null ? imageD.large : imageD.resource;
            case HIRES:
                return imageD.hires != null ? imageD.hires : imageD.resource;
            default:
                return imageD.resource;
        }
    }

    public static String getAbsoluteUrl(ResourceD resource, Size size) {
        String url = getUrl(resource, size);
        if(url.startsWith("http://"))
            return url;
        return Config.BASE_URL + url;
    }
}
