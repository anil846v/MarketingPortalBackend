package com.example.visited.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.visited.entitys.JWT;
import com.example.visited.entitys.User;

@Repository
public interface JWTTokenRepository extends JpaRepository<JWT, Integer> {
	 Optional<JWT> findByToken(String token);

	    JWT findByUser(User user);

	    // Corrected version using actual field name
	    JWT findByUser_UserId(Integer userId);

	    @Transactional
	    @Modifying
	    void deleteByUser(User user);

	    @Transactional
	    @Modifying
	    void deleteByUser_UserId(Integer userId);
}

////Wrong field names (must match entity fields)
//findByuserName  // ❌ Must be findByUsername (camelCase)
//
////Wrong keywords
//findWhereNameEq // ❌ Must be findByName
//
////Mixed case
//FindByName      // ❌ Must be camelCase


//| Prefix    | Purpose       | Return Type      | Generated SQL                              |
//| --------- | ------------- | ---------------- | ------------------------------------------ |
//| findBy    | Find single   | Optional<T>, T   | SELECT * FROM table WHERE field = ?        |
//| findAllBy | Find multiple | List<T>, Page<T> | SELECT * FROM table WHERE field = ?        |
//| deleteBy  | Delete        | void             | DELETE FROM table WHERE field = ?          |
//| countBy   | Count         | long, int        | SELECT COUNT(*) FROM table WHERE field = ? |
//| existsBy  | Exists        | boolean          | SELECT EXISTS(...) WHERE field = ?         |


//| Keyword      | Sample                 | SQL Generated               |
//| ------------ | ---------------------- | --------------------------- |
//| And          | findByNameAndAge       | WHERE name = ? AND age = ?  |
//| Or           | findByNameOrEmail      | WHERE name = ? OR email = ? |
//| Is           | findByNameIs           | WHERE name = ?              |
//| Equals       | findByNameEquals       | WHERE name = ?              |
//| Between      | findByAgeBetween       | WHERE age BETWEEN ? AND ?   |
//| LessThan     | findByAgeLessThan      | WHERE age < ?               |
//| GreaterThan  | findByAgeGreaterThan   | WHERE age > ?               |
//| Like         | findByNameLike         | WHERE name LIKE ?           |
//| StartingWith | findByNameStartingWith | WHERE name LIKE ?%          |
//| EndingWith   | findByNameEndingWith   | WHERE name LIKE ?%          |
//| Containing   | findByNameContaining   | WHERE name LIKE %?%         |
//| In           | findByIdIn             | WHERE id IN (?,?)           |
//| Not          | findByActiveNot        | WHERE active != ?           |
//| IsNull       | findByNameIsNull       | WHERE name IS NULL          |
//| IsNotNull    | findByNameIsNotNull    | WHERE name IS NOT NULL      |
//| True         | findByActiveTrue       | WHERE active = true         |
//| False        | findByActiveFalse      | WHERE active = false        |



//@Repository
//public interface UserRepository extends JpaRepository<User, Integer> {
//    // Single result
//    Optional<User> findByUsername(String username);
//    Optional<User> findByEmailOrPhone(String email, String phone);
//    
//    // Multiple results
//    List<User> findByRole(Role role);
//    List<User> findByActiveTrueOrderByNameAsc();
//    
//    // Count
//    long countByRole(Role role);
//    
//    // Delete
//    void deleteByUsername(String username);
//    void deleteByRole(Role role);
//    
//    // Complex
//    List<User> findByAddressCityAndActiveTrue(String city, boolean active);
//}
