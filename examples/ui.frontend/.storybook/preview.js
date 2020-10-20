import '../src/stories/styles/main.scss';

export const parameters = {
    actions: { argTypesRegex: '^on[A-Z].*' },
    cifConfig: {
        storeView: 'default',
        graphqlEndpoint: '/apps/cif-components-examples/graphql'
    }
};
