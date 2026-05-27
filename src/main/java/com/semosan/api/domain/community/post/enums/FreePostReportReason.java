package com.semosan.api.domain.community.post.enums;

public enum FreePostReportReason {
    SPAM("스팸"),
    ABUSE("욕설/혐오"),
    OBSCENE("음란/부적절"),
    FALSE_INFO("허위정보"),
    ETC("기타");

    private final String label;

    FreePostReportReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
