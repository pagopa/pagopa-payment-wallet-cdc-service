const { MongoClient } = require('mongodb');
const fs = require('fs');
const path = require('path');

async function main() {
    const uri = 'mongodb://mongodb:27017/?replicaSet=rs0';
    const client = new MongoClient(uri);

    try {
        await client.connect();

        const database = client.db('payment-wallet');
        const collection = database.collection('payment-wallets-log-events');

        const filePath = path.join(__dirname, 'data.json');
        const data = JSON.parse(fs.readFileSync(filePath, 'utf8'))

        const result = await collection.insertMany(data);
        console.log(`${result.insertedCount} events were inserted`);
    } catch (err) {
        console.error(err);
    } finally {
        await client.close();
    }
}

main().catch(console.error);
