package com.marek.book.network.book;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BorrowedBookResponse {
    private Integer id;
    private String title;
    private String authorName;
    private String isbn;
    private double rating;
    private boolean returned;
    private boolean returnedApproved;


}
