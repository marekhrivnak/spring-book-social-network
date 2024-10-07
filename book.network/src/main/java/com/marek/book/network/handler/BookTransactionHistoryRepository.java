package com.marek.book.network.handler;

import com.marek.book.network.history.BookTransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookTransactionHistoryRepository extends JpaRepository<BookTransactionHistory, Integer> {

    @Query("""
                SELECT history
                FROM BookTransactionHistory history
                WHERE history.user.id = :id
                """)
    Page<BookTransactionHistory> findAllBorrowedBooks(Pageable pageable, Integer id);

    @Query("""
                SELECT history
                FROM BookTransactionHistory history
                WHERE history.book.owner.id = :id
                """)
    Page<BookTransactionHistory> findAllReturnedBooks(Pageable pageable, Integer id);

    @Query("""
                SELECT (COUNT(*) > 0) AS isBorrowed
                FROM BookTransactionHistory history
                WHERE history.user.id = :userId
                AND history.book.id = :bookId
                AND history.returnApproved = false
            """)
    boolean isAlreadyBorrowsByUser(Integer bookId, Integer userId);

    @Query("""
                SELECT transaction
                FROM BookTransactionHistory  transaction
                WHERE transaction.user.id = :id
                AND transaction.book.id = :bookId
                AND transaction.returned = false
                AND transaction.returnApproved = false
            """)
    Optional<BookTransactionHistory> findByBookIdAndUserId(Integer bookId, Integer id);

    @Query("""
                SELECT transaction
                FROM BookTransactionHistory  transaction
                WHERE transaction.book.owner.id = :id
                AND transaction.book.id = :bookId
                AND transaction.returned = true
                AND transaction.returnApproved = false
            """)
    Optional<BookTransactionHistory> findByBookIdAndOwnerId(Integer bookId, Integer id);
}
