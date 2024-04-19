package co.istad.elearning.features.citycountry;

import co.istad.elearning.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country,Integer> {
    Optional<Country> findByName(String name);
}