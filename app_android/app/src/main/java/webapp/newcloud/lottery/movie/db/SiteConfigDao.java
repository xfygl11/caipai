package webapp.newcloud.lottery.movie.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import webapp.newcloud.lottery.movie.model.SiteConfig;

@Dao
public interface SiteConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SiteConfig siteConfig);

    @Update
    void update(SiteConfig siteConfig);

    @Delete
    void delete(SiteConfig siteConfig);

    @Query("DELETE FROM site_configs WHERE id = :id")
    void deleteById(String id);

    @Query("SELECT * FROM site_configs")
    List<SiteConfig> getAll();

    @Query("SELECT * FROM site_configs WHERE warehouse_id = :warehouseId")
    List<SiteConfig> getByWarehouseId(String warehouseId);

    @Query("SELECT * FROM site_configs WHERE source_type = :sourceType")
    List<SiteConfig> getBySourceType(String sourceType);

    @Query("SELECT * FROM site_configs WHERE id = :id LIMIT 1")
    SiteConfig getById(String id);

    @Query("DELETE FROM site_configs WHERE warehouse_id = :warehouseId")
    void deleteByWarehouseId(String warehouseId);
}
