package com.investwise.user.repository.mongo;

import com.investwise.user.model.ContactMessage;
import com.investwise.user.model.Enums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactMessageRepository extends MongoRepository<ContactMessage, String> {

    Page<ContactMessage> findByStatus(Enums.ContactStatus status, Pageable pageable);

    long countByStatus(Enums.ContactStatus status);

    @Query("""
           { '$or': [ { 'name':    { '$regex': ?0, '$options': 'i' } },
                      { 'email':   { '$regex': ?0, '$options': 'i' } },
                      { 'subject': { '$regex': ?0, '$options': 'i' } } ] }
           """)
    Page<ContactMessage> search(String keyword, Pageable pageable);
}
