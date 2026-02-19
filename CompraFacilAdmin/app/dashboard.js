import React, { useEffect, useState } from 'react';
import { View, FlatList, StyleSheet } from 'react-native';
import { List, FAB, IconButton, ActivityIndicator, Divider } from 'react-native-paper';
import { useRouter } from 'expo-router';
import { supabase } from '../src/lib/supabase';

export default function Dashboard() {
  const router = useRouter();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProducts();
  }, []);

  async function fetchProducts() {
    setLoading(true);
    const { data, error } = await supabase.from('products').select('*').order('created_at', { ascending: false });
    if (data) setProducts(data);
    setLoading(false);
  }

  async function deleteProduct(id) {
    const { error } = await supabase.from('products').delete().eq('id', id);
    if (!error) fetchProducts();
  }

  return (
    <View style={styles.container}>
      {loading ? (
        <ActivityIndicator animating={true} color="#007bff" style={{ marginTop: 20 }} />
      ) : (
        <FlatList
          data={products}
          keyExtractor={item => item.id}
          renderItem={({ item }) => (
            <List.Item
              title={item.name}
              description={`R$ ${item.price.toFixed(2)}`}
              left={props => <List.Icon {...props} icon="package-variant" />}
              right={props => (
                <View style={{ flexDirection: 'row' }}>
                  <IconButton icon="pencil" onPress={() => router.push({ pathname: '/add-product', params: { id: item.id } })} />
                  <IconButton icon="delete" iconColor="red" onPress={() => deleteProduct(item.id)} />
                </View>
              )}
            />
          )}
          ItemSeparatorComponent={Divider}
        />
      )}

      <FAB
        icon="plus"
        style={styles.fab}
        onPress={() => router.push('/add-product')}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    backgroundColor: '#007bff',
  },
});
