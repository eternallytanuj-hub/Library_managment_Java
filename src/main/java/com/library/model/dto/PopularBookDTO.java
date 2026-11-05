package com.library.model.dto;

/**
 * Popular book leaderboard ranking DTO.
 */
public class PopularBookDTO extends com.library.dao.dto.PopularBookDTO {
    private static final long serialVersionUID = 1L;

    public PopularBookDTO() {
        super();
    }

    public PopularBookDTO(int bookId, String title, String author, String genre, int borrowCount) {
        super(bookId, title, author, genre, borrowCount);
    }
}
