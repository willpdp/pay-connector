package uk.gov.pay.connector.dao;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import uk.gov.pay.connector.model.domain.CardTypeEntity;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Transactional
public class CardTypeDao extends JpaDao<CardTypeEntity> {

    @Inject
    public CardTypeDao(final Provider<EntityManager> entityManager) {
        super(entityManager);
    }

    public Optional<CardTypeEntity> findById(UUID id) {
        return super.findById(CardTypeEntity.class, id);
    }

    public List<CardTypeEntity> findAll() {
        String query = "SELECT ct FROM CardTypeEntity ct";

        return super.entityManager.get()
                .createQuery(query, CardTypeEntity.class)
                .getResultList();
    }
}