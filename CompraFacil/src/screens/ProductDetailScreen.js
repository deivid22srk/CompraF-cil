import React from 'react';
import { StyleSheet, View, ScrollView } from 'react-native';
import { Card, Title, Paragraph, Button, Divider, Text } from 'react-native-paper';

export default function ProductDetailScreen({ route }) {
  const { product } = route.params;

  return (
    <ScrollView style={styles.container}>
      <Card style={styles.card}>
        <Card.Cover source={{ uri: product.image_url || 'https://via.placeholder.com/300' }} style={styles.image} />
        <Card.Content>
          <Title style={styles.name}>{product.name}</Title>
          <Paragraph style={styles.price}>R$ {parseFloat(product.price).toFixed(2)}</Paragraph>
          <Divider style={styles.divider} />
          <Title style={styles.descriptionTitle}>Descrição</Title>
          <Paragraph style={styles.description}>{product.description || 'Sem descrição disponível.'}</Paragraph>
        </Card.Content>
        <Card.Actions style={styles.actions}>
          <Button
            mode="contained"
            onPress={() => console.log('Add to cart')}
            style={styles.button}
          >
            Adicionar ao Carrinho
          </Button>
        </Card.Actions>
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f6f6f6',
  },
  card: {
    margin: 10,
    borderRadius: 15,
    overflow: 'hidden',
  },
  image: {
    height: 300,
  },
  name: {
    fontSize: 24,
    marginTop: 15,
  },
  price: {
    fontSize: 20,
    color: '#6200ee',
    fontWeight: 'bold',
    marginBottom: 10,
  },
  divider: {
    marginVertical: 15,
  },
  descriptionTitle: {
    fontSize: 18,
  },
  description: {
    fontSize: 16,
    color: '#666',
    lineHeight: 24,
  },
  actions: {
    padding: 15,
  },
  button: {
    flex: 1,
    paddingVertical: 5,
  },
});
