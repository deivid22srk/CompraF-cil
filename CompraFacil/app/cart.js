import React from 'react';
import { View, StyleSheet, FlatList } from 'react-native';
import { Text, Title, Button, List, Divider } from 'react-native-paper';

export default function Cart() {
  // Simple mock cart for now
  const cartItems = [];

  return (
    <View style={styles.container}>
      {cartItems.length === 0 ? (
        <View style={styles.empty}>
          <Title>Seu carrinho está vazio</Title>
          <Text>Adicione produtos para começar a comprar!</Text>
        </View>
      ) : (
        <FlatList
          data={cartItems}
          keyExtractor={item => item.id}
          renderItem={({ item }) => (
            <List.Item
              title={item.name}
              description={`R$ ${item.price}`}
              right={props => <Text {...props}>x{item.quantity}</Text>}
            />
          )}
          ItemSeparatorComponent={Divider}
        />
      )}

      <View style={styles.footer}>
        <Divider />
        <View style={styles.totalRow}>
          <Text variant="titleMedium">Total:</Text>
          <Text variant="titleLarge" style={styles.totalText}>R$ 0,00</Text>
        </View>
        <Button mode="contained" disabled={cartItems.length === 0} style={styles.checkoutBtn}>
          Finalizar Compra
        </Button>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  empty: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  footer: {
    padding: 20,
    backgroundColor: '#f9f9f9',
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 15,
  },
  totalText: {
    color: '#6200ee',
    fontWeight: 'bold',
  },
  checkoutBtn: {
    paddingVertical: 5,
  }
});
