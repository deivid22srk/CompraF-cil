import React, { useEffect, useState } from 'react';
import { StyleSheet, View, FlatList, Alert } from 'react-native';
import { List, Avatar, IconButton, FAB, ActivityIndicator, Divider, Text, Searchbar, useTheme } from 'react-native-paper';
import { supabase } from '../lib/supabase';

export default function DashboardScreen({ navigation }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredProducts, setFilteredProducts] = useState([]);
  const theme = useTheme();

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
      'Confirmar Exclusão',
      'Tem certeza que deseja remover este produto permanentemente?',
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
      titleStyle={styles.itemTitle}
      description={`R$ ${parseFloat(item.price).toFixed(2)}`}
      descriptionStyle={styles.itemPrice}
      left={props => (
        <Avatar.Image
          {...props}
          size={56}
          source={{ uri: item.image_url || 'https://via.placeholder.com/56' }}
          style={styles.avatar}
        />
      )}
      right={props => (
        <View style={styles.actions}>
          <IconButton
            icon="pencil-outline"
            onPress={() => navigation.navigate('AddProduct', { product: item })}
          />
          <IconButton
            icon="trash-can-outline"
            onPress={() => deleteProduct(item.id)}
            iconColor={theme.colors.error}
          />
        </View>
      )}
      style={styles.listItem}
    />
  );

  return (
    <View style={styles.container}>
      <Searchbar
        placeholder="Pesquisar por nome..."
        onChangeText={onChangeSearch}
        value={searchQuery}
        style={styles.searchBar}
        elevation={0}
      />
      {loading && products.length === 0 ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color={theme.colors.primary} />
        </View>
      ) : (
        <FlatList
          data={filteredProducts}
          renderItem={renderItem}
          keyExtractor={item => item.id}
          ItemSeparatorComponent={() => <Divider style={styles.divider} />}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
        />
      )}
      <FAB
        icon="plus"
        style={[styles.fab, { backgroundColor: theme.colors.secondary }]}
        onPress={() => navigation.navigate('AddProduct')}
        label="Adicionar Produto"
        color="#000"
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
    margin: 16,
    backgroundColor: '#FFFFFF',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#E9ECEF',
  },
  list: {
    paddingBottom: 100,
    backgroundColor: '#FFFFFF',
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
  },
  listItem: {
    paddingVertical: 8,
    paddingHorizontal: 16,
  },
  itemTitle: {
    fontWeight: '600',
    fontSize: 16,
  },
  itemPrice: {
    color: '#6C757D',
  },
  avatar: {
    backgroundColor: '#F1F3F5',
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  divider: {
    marginHorizontal: 16,
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    borderRadius: 16,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  }
});
