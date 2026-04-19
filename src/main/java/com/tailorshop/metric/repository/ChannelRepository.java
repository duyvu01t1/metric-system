package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Channel Repository
 */
@Repository
public interface ChannelRepository extends JpaRepository<Channel, Long> {

    Optional<Channel> findByChannelCode(String channelCode);

    List<Channel> findByIsActiveTrueOrderBySortOrderAsc();

    boolean existsByChannelCode(String channelCode);
}
