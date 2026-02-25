package com.plog.domain.hashtag.entity

import com.plog.global.jpa.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "hashtag")
class HashTag  (

    var name: String
) : BaseEntity()