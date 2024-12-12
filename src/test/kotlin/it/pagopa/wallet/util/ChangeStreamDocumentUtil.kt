package it.pagopa.wallet.util

import com.mongodb.client.model.changestream.ChangeStreamDocument
import java.util.UUID
import org.bson.BsonDocument
import org.bson.Document
import org.springframework.data.mongodb.core.ChangeStreamEvent
import org.springframework.data.mongodb.core.convert.MongoConverter

class ChangeStreamDocumentUtil {
    companion object {
        fun getChangeStreamEvent(
            expectedDocument: Document,
            mongoConverter: MongoConverter
        ): ChangeStreamEvent<BsonDocument> {
            return ChangeStreamEvent(
                ChangeStreamDocument(
                    null,
                    BsonDocument(),
                    null,
                    null,
                    expectedDocument,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                ),
                BsonDocument::class.java,
                mongoConverter
            )
        }

        fun getDocument(): Document {
            return getDocument("testWallet", "testEvent", "testTimestamp")
        }

        fun getDocument(walletId: String, classT: String, timestamp: String?): Document {
            return Document("walletId", walletId)
                .append("_class", classT)
                .append("timestamp", timestamp)
                .append("_id", UUID.randomUUID().toString())
        }
    }
}
