import React, { useEffect, useState } from 'react';
import { StyleSheet, View, FlatList, Alert } from 'react-native';
import { List, Avatar, IconButton, FAB, ActivityIndicator, Divider, Text, Searchbar } from 'react-native-paper';
import { supabase } from '../lib/supabase';

export default function DashboardScreen({ navigation }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredProducts, setFilteredProducts] = useState([]);

  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', () => {
      fetchProducts();
    });
    return unsubscribe;
  }, [navigation]);

  async function fetchProducts() {
    setLoading(true);
    try {
      const { data, error } = await supabase
        .from('products')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) throw error;
      setProducts(data);
      setFilteredProducts(data);
    } catch (error) {
      console.error('Error:', error.message);
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

  async function deleteProduct(id) {
    Alert.alert(
      'Confirmar',
      'Deseja realmente excluir este produto?',
      [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Excluir', style: 'destructive', onPress: async () => {
          const { error } = await supabase.from('products').delete().eq('id', id);
          if (error) Alert.alert('Erro', error.message);
          else fetchProducts();
        }}
      ]
    );
  }

  const renderItem = ({ item }) => (
    <List.Item
      title={item.name}
      description={`R$ ${parseFloat(item.price).toFixed(2)}`}
      left={props => <Avatar.Image {...props} size={50} source={{ uri: item.image_url || 'https://via.placeholder.com/50' }} />}
      right={props => (
        <View style={styles.actions}>
          <IconButton
            icon="pencil"
            onPress={() => navigation.navigate('AddProduct', { product: item })}
          />
          <IconButton
            icon="delete"
            onPress={() => deleteProduct(item.id)}
            iconColor="#dc3545"
          />
        </View>
      )}
      style={styles.listItem}
    />
  );

  return (
    <View style={styles.container}>
      <Searchbar
        placeholder="Pesquisar..."
        onChangeText={onChangeSearch}
        value={searchQuery}
        style={styles.searchBar}
      />
      {loading && products.length === 0 ? (
        <ActivityIndicator size="large" color="#333" style={{ marginTop: 20 }} />
      ) : (
        <FlatList
          data={filteredProducts}
          renderItem={renderItem}
          keyExtractor={item => item.id}
          ItemSeparatorComponent={Divider}
          contentContainerStyle={styles.list}
        />
      )}
      <FAB
        icon="plus"
        style={styles.fab}
        onPress={() => navigation.navigate('AddProduct')}
        label="Novo Produto"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  searchBar: {
    margin: 10,
    backgroundColor: '#f0f0f0',
  },
  list: {
    paddingBottom: 80,
  },
  listItem: {
    paddingVertical: 10,
  },
  actions: {
    flexDirection: 'row',
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    backgroundColor: '#28a745',
  }
});
