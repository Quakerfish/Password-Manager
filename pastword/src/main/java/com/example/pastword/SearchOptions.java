package com.example.pastword;

public class SearchOptions {

    public enum SortBy {
        NAME,
        USERNAME,
        FOLDER
    }

    private final String query;
    private final String folderFilter;
    private final SortBy sortBy;
    private final boolean ascending;

    public SearchOptions(String query, String folderFilter, SortBy sortBy, boolean ascending) {
        this.query = query == null ? "" : query;
        this.folderFilter = folderFilter == null ? "All folders" : folderFilter;
        this.sortBy = sortBy == null ? SortBy.NAME : sortBy;
        this.ascending = ascending;
    }

    public static SearchOptions defaults() {
        return new SearchOptions("", "All folders", SortBy.NAME, true);
    }

    public String getQuery() {
        return query;
    }

    public String getFolderFilter() {
        return folderFilter;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public boolean isAscending() {
        return ascending;
    }
}
