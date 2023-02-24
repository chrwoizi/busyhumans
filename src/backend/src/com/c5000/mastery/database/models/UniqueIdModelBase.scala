package com.c5000.mastery.database.models

import java.util.UUID
import com.c5000.mongopa.{MpaModelBase, MpaKey}

class UniqueIdModelBase extends MpaModelBase {

    @MpaKey var id: UUID = UUID.randomUUID()

}
