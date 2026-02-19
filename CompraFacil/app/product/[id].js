import React, { useEffect, useState } from 'react';
import { View, ScrollView, StyleSheet, Image } from 'react-native';
import { Text, Title, Paragraph, Button, ActivityIndicator } from 'react-native-paper';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { supabase } from '../../src/lib/supabase';

export default function ProductDetails() {
  const { id } = useLocalSearchParams();
  const router = useRouter();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProduct();
  }, [id]);

  async function fetchProduct() {
    const { data, error } = await supabase
      .from('products')
      .select('*, categories(name)')
      .eq('id', id)
      .single();

    if (data) setProduct(data);
    setLoading(false);
  }

  if (loading) return <ActivityIndicator style={{ flex: 1 }} />;
  if (!product) return <View style={styles.container}><Text>Produto não encontrado</Text></View>;

  return (
    <ScrollView style={styles.container}>
      <Image source={{ uri: product.image_url || 'https://via.placeholder.com/300' }} style={styles.image} />
      <View style={styles.content}>
        <Title style={styles.title}>{product.name}</Title>
        <Text style={styles.category}>{product.categories?.name || 'Sem categoria'}</Text>
        <Text style={styles.price}>R$ {product.price.toFixed(2)}</Text>
        <Paragraph style={styles.description}>{product.description}</Paragraph>

        <Button mode="contained" style={styles.button} onPress={() => {}}>
          Adicionar ao Carrinho
        </Button>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  image: {
    width: '100%',
    height: 300,
    resizeMode: 'cover',
  },
  content: {
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  category: {
    color: '#666',
    marginBottom: 10,
  },
  price: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#6200ee',
    marginVertical: 10,
  },
  description: {
    fontSize: 16,
    lineHeight: 24,
    color: '#333',
    marginVertical: 15,
  },
  button: {
    marginTop: 20,
    paddingVertical: 5,
  }
});
