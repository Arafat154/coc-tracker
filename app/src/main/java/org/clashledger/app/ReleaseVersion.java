package org.clashledger.app;

/** Numeric stable-release tags, with optional v prefix. Never compare versions lexically. */
public final class ReleaseVersion {
    public static int compare(String a,String b) {
        int[] left=parts(a),right=parts(b);
        for(int i=0;i<3;i++){int diff=Integer.compare(left[i],right[i]);if(diff!=0)return diff;}
        return 0;
    }
    private static int[] parts(String value) {
        if(value==null||!value.matches("v?\\d{1,6}\\.\\d{1,6}\\.\\d{1,6}"))throw new IllegalArgumentException("Unsupported release version");
        String[] parts=value.replaceFirst("^v","").split("\\.");
        return new int[]{Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2])};
    }
    private ReleaseVersion() { }
}
