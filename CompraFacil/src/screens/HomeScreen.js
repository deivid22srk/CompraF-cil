import React, { useEffect, useState } from 'react';
import { StyleSheet, View, FlatList, ActivityIndicator, Dimensions } from 'react-native';
import { Card, Title, Paragraph, Searchbar, Text, useTheme } from 'react-native-paper';
import { supabase } from '../lib/supabase';

const { width } = Dimensions.get('window');

export default function HomeScreen({ navigation }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredProducts, setFilteredProducts] = useState([]);
  const theme = useTheme();

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
      mode="elevated"
    >
      <Card.Cover source={{ uri: item.image_url || 'https://via.placeholder.com/150' }} style={styles.cardImage} />
      <Card.Content style={styles.cardContent}>
        <Text variant="titleMedium" numberOfLines={1} style={styles.title}>{item.name}</Text>
        <Text variant="bodyLarge" style={[styles.price, { color: theme.colors.primary }]}>
          R$ {parseFloat(item.price).toFixed(2)}
        </Text>
      </Card.Content>
    </Card>
  );

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
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
        elevation={0}
      />
      <FlatList
        data={filteredProducts}
        renderItem={renderItem}
        keyExtractor={item => item.id}
        numColumns={2}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  searchBar: {
    margin: 15,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E9ECEF',
  },
  list: {
    padding: 10,
  },
  card: {
    flex: 1,
    margin: 6,
    borderRadius: 16,
    backgroundColor: '#FFFFFF',
    overflow: 'hidden',
  },
  cardImage: {
    height: 160,
  },
  cardContent: {
    padding: 12,
  },
  title: {
    fontWeight: '600',
    marginBottom: 4,
  },
  price: {
    fontWeight: '700',
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  }
});
