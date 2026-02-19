import React from 'react';
import { StyleSheet, View, ScrollView, Image, Dimensions } from 'react-native';
import { Title, Paragraph, Button, Divider, Text, Surface, useTheme } from 'react-native-paper';

const { width } = Dimensions.get('window');

export default function ProductDetailScreen({ route }) {
  const { product } = route.params;
  const theme = useTheme();

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      <Image
        source={{ uri: product.image_url || 'https://via.placeholder.com/400' }}
        style={styles.image}
      />

      <Surface style={styles.content} elevation={0}>
        <View style={styles.header}>
          <Text variant="headlineSmall" style={styles.name}>{product.name}</Text>
          <Text variant="headlineMedium" style={[styles.price, { color: theme.colors.primary }]}>
            R$ {parseFloat(product.price).toFixed(2)}
          </Text>
        </View>

        <Divider style={styles.divider} />

        <Text variant="titleMedium" style={styles.sectionTitle}>Descrição</Text>
        <Text variant="bodyLarge" style={styles.description}>
          {product.description || 'Sem descrição disponível.'}
        </Text>

        <View style={styles.footer}>
          <Button
            mode="contained"
            onPress={() => console.log('Add to cart')}
            style={styles.button}
            contentStyle={styles.buttonContent}
          >
            Adicionar ao Carrinho
          </Button>
        </View>
      </Surface>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  image: {
    width: width,
    height: width,
  },
  content: {
    padding: 24,
    borderTopLeftRadius: 30,
    borderTopRightRadius: 30,
    marginTop: -30,
    backgroundColor: '#FFFFFF',
  },
  header: {
    marginBottom: 20,
  },
  name: {
    fontWeight: '700',
    color: '#212529',
  },
  price: {
    fontWeight: '800',
    marginTop: 8,
  },
  divider: {
    marginBottom: 24,
  },
  sectionTitle: {
    fontWeight: '600',
    marginBottom: 8,
    color: '#495057',
  },
  description: {
    color: '#6C757D',
    lineHeight: 24,
    marginBottom: 100,
  },
  footer: {
    position: 'absolute',
    bottom: 24,
    left: 24,
    right: 24,
  },
  button: {
    borderRadius: 12,
  },
  buttonContent: {
    height: 54,
  },
});
