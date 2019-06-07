package com.c5000.mastery.database.models

import com.c5000.mongopa.MpaModel
import org.joda.time.DateTime
import java.util.UUID

@MpaModel
class ListItemM extends UniqueIdModelBase {
    def this(id: UUID) {
        this()
        this.id = id
    }

    override def equals(other: Any): Boolean = {
        if (other == null)
            return false
        if(other.isInstanceOf[ListItemM])
            return id == other.asInstanceOf[ListItemM].id
        return false
    }
}
