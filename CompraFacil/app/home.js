import React, { useEffect, useState } from 'react';
import { View, FlatList, StyleSheet, Image, ScrollView, TouchableOpacity } from 'react-native';
import { Card, Text, Searchbar, ActivityIndicator, IconButton, FAB } from 'react-native-paper';
import { useRouter } from 'expo-router';
import { supabase } from '../src/lib/supabase';

export default function Home() {
  const router = useRouter();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    setLoading(true);
    const { data: catData } = await supabase.from('categories').select('*');
    const { data: prodData } = await supabase.from('products').select('*');

    setCategories(catData || []);
    setProducts(prodData || []);
    setLoading(false);
  }

  const onChangeSearch = query => setSearchQuery(query);

  const renderProduct = ({ item }) => (
    <Card style={styles.card} onPress={() => router.push(`/product/${item.id}`)}>
      <Card.Cover source={{ uri: item.image_url || 'https://via.placeholder.com/150' }} />
      <Card.Content style={styles.cardContent}>
        <Text variant="titleMedium" numberOfLines={1}>{item.name}</Text>
        <Text variant="bodyMedium" style={styles.price}>R$ {item.price.toFixed(2)}</Text>
      </Card.Content>
      <Card.Actions>
        <IconButton icon="cart-plus" onPress={() => {}} />
      </Card.Actions>
    </Card>
  );

  return (
    <View style={styles.container}>
      <Searchbar
        placeholder="Buscar produtos..."
        onChangeText={onChangeSearch}
        value={searchQuery}
        style={styles.searchbar}
      />

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.categoriesContainer}>
        {categories.map(cat => (
          <TouchableOpacity key={cat.id} style={styles.categoryBadge}>
            <Text style={styles.categoryText}>{cat.name}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {loading ? (
        <ActivityIndicator animating={true} color="#6200ee" style={{ marginTop: 20 }} />
      ) : (
        <FlatList
          data={products.filter(p => p.name.toLowerCase().includes(searchQuery.toLowerCase()))}
          renderItem={renderProduct}
          keyExtractor={item => item.id}
          numColumns={2}
          contentContainerStyle={styles.productList}
        />
      )}

      <FAB
        icon="cart"
        style={styles.fab}
        onPress={() => router.push('/cart')}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  searchbar: {
    margin: 10,
    elevation: 2,
  },
  categoriesContainer: {
    paddingHorizontal: 10,
    marginBottom: 10,
    maxHeight: 50,
  },
  categoryBadge: {
    backgroundColor: '#fff',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 20,
    marginRight: 10,
    borderWidth: 1,
    borderColor: '#e0e0e0',
    height: 35,
  },
  categoryText: {
    fontSize: 14,
    color: '#333',
  },
  productList: {
    padding: 5,
  },
  card: {
    flex: 0.5,
    margin: 5,
    elevation: 3,
    backgroundColor: '#fff',
  },
  cardContent: {
    paddingTop: 10,
  },
  price: {
    fontWeight: 'bold',
    color: '#6200ee',
    marginTop: 5,
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    backgroundColor: '#6200ee',
  },
});
