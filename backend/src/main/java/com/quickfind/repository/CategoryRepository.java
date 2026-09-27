package com.quickfind.repository;

import com.quickfind.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("select c from Category c left join fetch c.parent order by c.name")
    List<Category> findAllWithParent();
}
