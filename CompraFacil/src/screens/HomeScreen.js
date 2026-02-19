import React, { useEffect, useState } from 'react';
import { StyleSheet, View, FlatList, ActivityIndicator } from 'react-native';
import { Card, Title, Paragraph, Searchbar, FAB, Text } from 'react-native-paper';
import { supabase } from '../lib/supabase';

export default function HomeScreen({ navigation }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredProducts, setFilteredProducts] = useState([]);

  useEffect(() => {
    fetchProducts();
  }, []);

  async function fetchProducts() {
    try {
      const { data, error } = await supabase
        .from('products')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) throw error;
      setProducts(data);
      setFilteredProducts(data);
    } catch (error) {
      console.error('Error fetching products:', error.message);
    } finally {
      setLoading(false);
    }
  }

  const onChangeSearch = query => {
    setSearchQuery(query);
    const filtered = products.filter(product =>
      product.name.toLowerCase().includes(query.toLowerCase())
    );
    setFilteredProducts(filtered);
  };

  const renderItem = ({ item }) => (
    <Card
      style={styles.card}
      onPress={() => navigation.navigate('ProductDetail', { product: item })}
    >
      <Card.Cover source={{ uri: item.image_url || 'https://via.placeholder.com/150' }} />
      <Card.Content style={styles.cardContent}>
        <Title numberOfLines={1} style={styles.title}>{item.name}</Title>
        <Paragraph style={styles.price}>R$ {parseFloat(item.price).toFixed(2)}</Paragraph>
      </Card.Content>
    </Card>
  );

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#6200ee" />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Searchbar
        placeholder="Buscar produtos..."
        onChangeText={onChangeSearch}
        value={searchQuery}
        style={styles.searchBar}
      />
      <FlatList
        data={filteredProducts}
        renderItem={renderItem}
        keyExtractor={item => item.id}
        numColumns={2}
        contentContainerStyle={styles.list}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f6f6f6',
  },
  searchBar: {
    margin: 10,
    elevation: 4,
  },
  list: {
    padding: 5,
  },
  card: {
    flex: 0.5,
    margin: 5,
    elevation: 3,
    borderRadius: 12,
  },
  cardContent: {
    paddingVertical: 10,
  },
  title: {
    fontSize: 16,
    lineHeight: 20,
  },
  price: {
    fontSize: 14,
    color: '#6200ee',
    fontWeight: 'bold',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  }
});
