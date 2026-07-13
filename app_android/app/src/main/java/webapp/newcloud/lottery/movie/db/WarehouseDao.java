package webapp.newcloud.lottery.movie.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import webapp.newcloud.lottery.movie.model.Warehouse;

import java.util.List;

@Dao
public interface WarehouseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Warehouse warehouse);

    @Update
    void update(Warehouse warehouse);

    @Delete
    void delete(Warehouse warehouse);

    @Query("DELETE FROM warehouses WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM warehouses ORDER BY created_at DESC")
    List<Warehouse> getAll();

    @Query("SELECT * FROM warehouses WHERE id = :id LIMIT 1")
    Warehouse getById(String id);
}
