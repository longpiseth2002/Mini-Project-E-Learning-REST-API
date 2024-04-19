package co.istad.elearning.features.citycountry;

import co.istad.elearning.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {
    List<City> findCityByCountry_Iso(String iso);

}
