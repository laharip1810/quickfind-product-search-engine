package com.quickfind.exception;

/** Invalid search, sort, filter or paging parameters. */
public class InvalidSearchRequestException extends QuickFindException {

    public InvalidSearchRequestException(ErrorCode code, String message) {
        super(code, message);
    }

    public static InvalidSearchRequestException invalidSort(String message) {
        return new InvalidSearchRequestException(ErrorCode.INVALID_SORT, message);
    }

    public static InvalidSearchRequestException invalidFilter(String message) {
        return new InvalidSearchRequestException(ErrorCode.INVALID_FILTER, message);
    }

    public static InvalidSearchRequestException invalidRequest(String message) {
        return new InvalidSearchRequestException(ErrorCode.INVALID_SEARCH_REQUEST, message);
    }
}
